package com.yunong.module.knowledge.controller;

import com.yunong.common.AuditLog;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.module.knowledge.entity.KnowledgeDocument;
import com.yunong.module.knowledge.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.yunong.security.UserDetailsImpl;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库", description = "知识文档管理、RAG 语义检索")
public class KnowledgeController {

    private final KnowledgeService service;

    @PostMapping("/documents")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'COOP_MANAGER', 'ADMIN')")
    @AuditLog(action = "上传知识文档")
    @Operation(summary = "上传知识文档")
    public R<KnowledgeDocument> create(@Valid @RequestBody KnowledgeDocument doc,
                                        @AuthenticationPrincipal UserDetailsImpl principal) {
        return R.ok(service.create(doc, principal.getUserId()));
    }

    @GetMapping("/documents")
    @Operation(summary = "文档列表")
    public R<PageResult<KnowledgeDocument>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return R.ok(service.list(page, size, category, keyword));
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "文档详情")
    public R<KnowledgeDocument> getById(@PathVariable String id) {
        return R.ok(service.getById(id));
    }

    @PutMapping("/documents/{id}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    @AuditLog(action = "更新知识文档")
    @Operation(summary = "更新文档")
    public R<KnowledgeDocument> update(@PathVariable String id, @RequestBody KnowledgeDocument update) {
        return R.ok(service.update(id, update));
    }

    @GetMapping("/search")
    @Operation(summary = "知识库搜索（关键词 + 向量语义）")
    public R<PageResult<KnowledgeDocument>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String embedding) {
        if (embedding != null && !embedding.isBlank()) {
            // pgvector 余弦相似度搜索
            return R.ok(service.vectorSearch(embedding, page, size));
        }
        // 兜底：关键词模糊搜索
        return R.ok(service.keywordSearch(q, page, size));
    }
}
