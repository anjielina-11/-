package com.yunong.module.model.controller;

import com.yunong.common.AuditLog;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.module.model.entity.ModelVersion;
import com.yunong.module.model.service.ModelVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/model-versions")
@RequiredArgsConstructor
@Tag(name = "模型管理", description = "AI 模型版本注册、部署、性能追踪")
public class ModelVersionController {

    private final ModelVersionService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    @AuditLog(action = "注册模型版本")
    @Operation(summary = "注册模型版本")
    public R<ModelVersion> create(@Valid @RequestBody ModelVersion mv) {
        return R.ok(service.create(mv));
    }

    @GetMapping
    @Operation(summary = "模型版本列表")
    public R<PageResult<ModelVersion>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) String status) {
        return R.ok(service.list(page, size, modelType, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "模型版本详情")
    public R<ModelVersion> getById(@PathVariable String id) {
        return R.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    @AuditLog(action = "更新模型版本")
    @Operation(summary = "更新模型版本")
    public R<ModelVersion> update(@PathVariable String id, @RequestBody ModelVersion mv) {
        return R.ok(service.update(id, mv));
    }

    @PostMapping("/{id}/deploy")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(action = "部署模型版本")
    @Operation(summary = "部署模型(管理员)")
    public R<ModelVersion> deploy(@PathVariable String id) {
        return R.ok(service.deploy(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(action = "删除模型版本")
    @Operation(summary = "删除模型版本(管理员)")
    public R<Void> delete(@PathVariable String id) {
        service.delete(id);
        return R.ok();
    }
}
