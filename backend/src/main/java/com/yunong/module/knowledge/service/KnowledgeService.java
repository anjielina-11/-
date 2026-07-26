package com.yunong.module.knowledge.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.integration.ai.AiServiceClient;
import com.yunong.integration.ai.dto.KnowledgeSyncRequest;
import com.yunong.module.knowledge.entity.KnowledgeDocument;
import com.yunong.module.knowledge.mapper.KnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private static final Set<String> VALID_STATUSES = Set.of("draft", "published", "archived");

    private final KnowledgeDocumentMapper mapper;
    private final AiServiceClient aiServiceClient;

    @Transactional
    public KnowledgeDocument create(KnowledgeDocument doc, String authorId) {
        String status = StrUtil.isBlank(doc.getStatus()) ? "draft" : doc.getStatus();
        validateStatus(status);
        doc.setAuthorId(authorId);
        doc.setVersion(1);
        doc.setStatus(status);
        mapper.insert(doc);
        if ("published".equals(status)) {
            syncPublished();
        }
        return doc;
    }

    public PageResult<KnowledgeDocument> list(int page, int size, String category, String keyword) {
        var wrapper = new LambdaQueryWrapper<KnowledgeDocument>();
        if (category != null) wrapper.eq(KnowledgeDocument::getCategory, category);
        if (keyword != null) wrapper.and(w -> w.like(KnowledgeDocument::getTitle, keyword)
                .or().like(KnowledgeDocument::getContent, keyword));
        wrapper.orderByDesc(KnowledgeDocument::getCreatedAt);
        var result = mapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public KnowledgeDocument getById(String id) {
        var doc = mapper.selectById(id);
        if (doc == null) throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        return doc;
    }

    @Transactional
    public KnowledgeDocument update(String id, KnowledgeDocument update) {
        var doc = getById(id);
        String previousStatus = doc.getStatus();
        String previousContent = doc.getContent();
        String previousTitle = doc.getTitle();
        String previousCategory = doc.getCategory();
        String previousTags = doc.getTags();
        String previousSourceUrl = doc.getSourceUrl();

        if (update.getTitle() != null) doc.setTitle(update.getTitle());
        if (update.getContent() != null) doc.setContent(update.getContent());
        if (update.getCategory() != null) doc.setCategory(update.getCategory());
        if (update.getTags() != null) doc.setTags(update.getTags());
        if (update.getSourceUrl() != null) doc.setSourceUrl(update.getSourceUrl());
        if (update.getStatus() != null) {
            validateStatus(update.getStatus());
            doc.setStatus(update.getStatus());
        }

        boolean versionChanged = !Objects.equals(previousContent, doc.getContent())
                || !Objects.equals(previousStatus, doc.getStatus());
        boolean synchronizedContentChanged = versionChanged
                || !Objects.equals(previousTitle, doc.getTitle())
                || !Objects.equals(previousCategory, doc.getCategory())
                || !Objects.equals(previousTags, doc.getTags())
                || !Objects.equals(previousSourceUrl, doc.getSourceUrl());
        if (versionChanged) {
            doc.setVersion((doc.getVersion() == null ? 1 : doc.getVersion()) + 1);
        }
        mapper.updateById(doc);

        if (synchronizedContentChanged
                && ("published".equals(previousStatus) || "published".equals(doc.getStatus()))) {
            syncPublished();
        }
        return doc;
    }

    @Transactional
    public KnowledgeDocument archive(String id) {
        var doc = getById(id);
        if ("archived".equals(doc.getStatus())) {
            return doc;
        }
        doc.setStatus("archived");
        doc.setVersion((doc.getVersion() == null ? 1 : doc.getVersion()) + 1);
        mapper.updateById(doc);
        syncPublished();
        return doc;
    }

    @Transactional(readOnly = true)
    public int syncPublished() {
        var published = mapper.selectList(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getStatus, "published")
                .orderByAsc(KnowledgeDocument::getCreatedAt));
        var documents = published.stream().map(this::toSyncDocument).toList();
        aiServiceClient.replaceKnowledge(new KnowledgeSyncRequest(documents));
        return documents.size();
    }

    private KnowledgeSyncRequest.Document toSyncDocument(KnowledgeDocument document) {
        return new KnowledgeSyncRequest.Document(
                document.getId(),
                document.getTitle(),
                document.getCategory(),
                document.getVersion(),
                document.getContent(),
                parseTags(document.getTags()),
                document.getStatus()
        );
    }

    private List<String> parseTags(String tags) {
        if (StrUtil.isBlank(tags)) {
            return List.of();
        }
        try {
            return JSONUtil.parseArray(tags).toList(String.class);
        } catch (Exception ignored) {
            return Arrays.stream(tags.split(","))
                    .map(String::trim)
                    .map(value -> value.replace("\"", ""))
                    .filter(value -> !value.isBlank())
                    .toList();
        }
    }

    private void validateStatus(String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "知识文档状态只能为 draft/published/archived");
        }
    }

    /** pgvector 余弦相似度搜索，embedding 格式如 [0.1,0.2,...] */
    public PageResult<KnowledgeDocument> vectorSearch(String queryEmbedding, int page, int size) {
        if (!queryEmbedding.matches("^\\[\\d.\\-, ]+\\]$")) {
            throw new IllegalArgumentException("Invalid embedding format");
        }
        String safeEmbedding = queryEmbedding.replace("'", "").replace("\"", "");
        var wrapper = new LambdaQueryWrapper<KnowledgeDocument>()
                .apply("embedding <=> ?::vector", safeEmbedding)
                .last("LIMIT " + size + " OFFSET " + (page - 1) * size);
        var records = mapper.selectList(wrapper);
        long total = mapper.selectCount(new LambdaQueryWrapper<KnowledgeDocument>()
                .isNotNull(KnowledgeDocument::getEmbedding));
        return PageResult.of(records, total);
    }

    /** 基于关键词检索（关键词匹配 + 结果按相关性排序） */
    public PageResult<KnowledgeDocument> keywordSearch(String query, int page, int size) {
        var wrapper = new LambdaQueryWrapper<KnowledgeDocument>()
                .and(w -> w.like(KnowledgeDocument::getTitle, query)
                        .or().like(KnowledgeDocument::getContent, query))
                .orderByDesc(KnowledgeDocument::getCreatedAt);
        var result = mapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }
}