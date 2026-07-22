package com.yunong.module.knowledge.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.knowledge.entity.KnowledgeDocument;
import com.yunong.module.knowledge.mapper.KnowledgeDocumentMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库", description = "知识文档管理和RAG检索")
public class KnowledgeController {

    private final KnowledgeDocumentMapper kdMapper;

    @PostMapping("/documents")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'COOP_MANAGER', 'ADMIN')")
    @Operation(summary = "上传知识文档")
    public R<KnowledgeDocument> create(@Valid @RequestBody KnowledgeDocument doc,
                                        @AuthenticationPrincipal UserDetails principal) {
        doc.setAuthorId(principal.getUsername());
        doc.setVersion(1);
        doc.setStatus("published");
        kdMapper.insert(doc);
        return R.ok(doc);
    }

    @GetMapping("/documents")
    @Operation(summary = "文档列表")
    public R<PageResult<KnowledgeDocument>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        var wrapper = new LambdaQueryWrapper<KnowledgeDocument>();
        if (category != null) wrapper.eq(KnowledgeDocument::getCategory, category);
        if (keyword != null) wrapper.and(w -> w.like(KnowledgeDocument::getTitle, keyword)
                .or().like(KnowledgeDocument::getContent, keyword));
        wrapper.orderByDesc(KnowledgeDocument::getCreatedAt);
        var result = kdMapper.selectPage(new Page<>(page, size), wrapper);
        return R.ok(PageResult.of(result.getRecords(), result.getTotal()));
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "文档详情")
    public R<KnowledgeDocument> getById(@PathVariable String id) {
        var doc = kdMapper.selectById(id);
        if (doc == null) throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        return R.ok(doc);
    }

    @PutMapping("/documents/{id}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    @Operation(summary = "更新文档")
    public R<KnowledgeDocument> update(@PathVariable String id, @RequestBody KnowledgeDocument update) {
        var doc = kdMapper.selectById(id);
        if (doc == null) throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        if (update.getTitle() != null) doc.setTitle(update.getTitle());
        if (update.getContent() != null) doc.setContent(update.getContent());
        if (update.getCategory() != null) doc.setCategory(update.getCategory());
        if (update.getTags() != null) doc.setTags(update.getTags());
        doc.setVersion(doc.getVersion() + 1);
        kdMapper.updateById(doc);
        return R.ok(doc);
    }

    @GetMapping("/search")
    @Operation(summary = "知识库语义搜索(RAG)")
    public R<PageResult<KnowledgeDocument>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        // 实际语义搜索由 pgvector 完成，此处先用关键词模糊匹配
        var wrapper = new LambdaQueryWrapper<KnowledgeDocument>()
                .and(w -> w.like(KnowledgeDocument::getTitle, q)
                        .or().like(KnowledgeDocument::getContent, q))
                .orderByDesc(KnowledgeDocument::getCreatedAt);
        var result = kdMapper.selectPage(new Page<>(page, size), wrapper);
        return R.ok(PageResult.of(result.getRecords(), result.getTotal()));
    }
}
