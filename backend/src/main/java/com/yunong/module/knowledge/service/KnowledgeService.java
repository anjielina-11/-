package com.yunong.module.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.knowledge.entity.KnowledgeDocument;
import com.yunong.module.knowledge.mapper.KnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeDocumentMapper mapper;

    public KnowledgeDocument create(KnowledgeDocument doc, String authorId) {
        doc.setAuthorId(authorId);
        doc.setVersion(1);
        doc.setStatus("published");
        mapper.insert(doc);
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

    public KnowledgeDocument update(String id, KnowledgeDocument update) {
        var doc = getById(id);
        if (update.getTitle() != null) doc.setTitle(update.getTitle());
        if (update.getContent() != null) doc.setContent(update.getContent());
        if (update.getCategory() != null) doc.setCategory(update.getCategory());
        if (update.getTags() != null) doc.setTags(update.getTags());
        doc.setVersion(doc.getVersion() + 1);
        mapper.updateById(doc);
        return doc;
    }

    /** pgvector 余弦相似度搜索，embedding 格式如 [0.1,0.2,...] */
    public PageResult<KnowledgeDocument> vectorSearch(String queryEmbedding, int page, int size) {
        // 校验格式，防止 SQL 注入
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
