package com.yunong.module.monitor.controller;

import com.yunong.common.R;
import com.yunong.module.monitor.service.MonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TECHNICIAN', 'COOP_MANAGER', 'ADMIN')")
@Tag(name = "系统监控", description = "模型性能、数据漂移、未知样本统计")
public class MonitorController {

    private final MonitorService service;

    @GetMapping("/overview")
    @Operation(summary = "系统概览")
    public R<Map<String, Object>> overview() {
        return R.ok(service.overview());
    }

    @GetMapping("/model-performance")
    @Operation(summary = "模型性能指标")
    public R<Map<String, Object>> modelPerformance() {
        return R.ok(service.modelPerformance());
    }

    @GetMapping("/data-drift")
    @Operation(summary = "数据漂移监控")
    public R<Map<String, Object>> dataDrift() {
        return R.ok(service.dataDrift());
    }

    @GetMapping("/unknown-samples")
    @Operation(summary = "未知样本统计")
    public R<Map<String, Object>> unknownSamples() {
        return R.ok(service.unknownSamples());
    }
}
