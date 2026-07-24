package com.yunong.module.farm.controller;

import com.yunong.common.AuditLog;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.module.farm.dto.FieldDTO;
import com.yunong.module.farm.entity.Farm;
import com.yunong.module.farm.entity.Field;
import com.yunong.module.farm.service.FarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.yunong.security.UserDetailsImpl;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/farms")
@RequiredArgsConstructor
@Tag(name = "农场管理", description = "农场和地块CRUD")
public class FarmController {

    private final FarmService service;

    @PostMapping
    @AuditLog(action = "创建农场")
    @Operation(summary = "创建农场")
    public R<Farm> create(@Valid @RequestBody Farm farm, @AuthenticationPrincipal UserDetailsImpl principal) {
        return R.ok(service.create(farm, principal.getUserId()));
    }

    @GetMapping
    @Operation(summary = "我的农场列表")
    public R<PageResult<Farm>> list(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(service.listByOwner(principal.getUserId(), page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "农场详情")
    public R<Farm> getById(@PathVariable String id,
                           @AuthenticationPrincipal UserDetailsImpl principal) {
        return R.ok(service.getById(id, principal.getUserId()));
    }

    @PutMapping("/{id}")
    @AuditLog(action = "更新农场")
    @Operation(summary = "更新农场")
    public R<Farm> update(@PathVariable String id, @RequestBody Farm update,
                          @AuthenticationPrincipal UserDetailsImpl principal) {
        return R.ok(service.update(id, update, principal.getUserId()));
    }

    @PostMapping("/{farmId}/fields")
    @AuditLog(action = "添加地块")
    @Operation(summary = "添加地块")
    public R<Field> addField(@PathVariable String farmId, @Valid @RequestBody Field field,
                             @AuthenticationPrincipal UserDetailsImpl principal) {
        return R.ok(service.addField(farmId, field, principal.getUserId()));
    }

    @GetMapping("/{farmId}/fields")
    @Operation(summary = "地块列表")
    public R<PageResult<FieldDTO>> listFields(@PathVariable String farmId,
                                              @AuthenticationPrincipal UserDetailsImpl principal) {
        var fields = service.listFields(farmId, principal.getUserId());
        List<FieldDTO> list = fields.stream()
                .map(f -> FieldDTO.from(f, null))
                .collect(Collectors.toList());
        return R.ok(PageResult.of(list, list.size()));
    }

    @PutMapping("/{farmId}/fields/{fieldId}")
    @AuditLog(action = "更新地块")
    @Operation(summary = "更新地块")
    public R<Field> updateField(@PathVariable String farmId, @PathVariable String fieldId,
                                @RequestBody Field update,
                                @AuthenticationPrincipal UserDetailsImpl principal) {
        return R.ok(service.updateField(farmId, fieldId, update, principal.getUserId()));
    }

    @DeleteMapping("/{farmId}/fields/{fieldId}")
    @AuditLog(action = "删除地块")
    @Operation(summary = "删除地块")
    public R<Void> deleteField(@PathVariable String farmId, @PathVariable String fieldId,
                               @AuthenticationPrincipal UserDetailsImpl principal) {
        service.deleteField(farmId, fieldId, principal.getUserId());
        return R.ok();
    }
}
