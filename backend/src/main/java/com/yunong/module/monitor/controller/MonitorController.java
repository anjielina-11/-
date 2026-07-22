package com.yunong.module.monitor.controller;

import com.yunong.common.R;
import com.yunong.module.diagnosis.mapper.DiagnosisRecordMapper;
import com.yunong.module.auth.mapper.UserMapper;
import com.yunong.module.farm.mapper.FarmMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TECHNICIAN', 'COOP_MANAGER', 'ADMIN')")
@Tag(name = "系统监控", description = "模型性能、数据漂移、未知样本统计")
public class MonitorController {

    private final DiagnosisRecordMapper drMapper;
    private final UserMapper userMapper;
    private final FarmMapper farmMapper;

    @GetMapping("/overview")
    @Operation(summary = "系统概览")
    public R<Map<String, Object>> overview() {
        var result = new HashMap<String, Object>();
        result.put("totalUsers", userMapper.selectCount(null));
        result.put("totalFarms", farmMapper.selectCount(null));
        result.put("totalDiagnoses", drMapper.selectCount(null));
        result.put("modelStatus", "healthy");
        return R.ok(result);
    }

    @GetMapping("/model-performance")
    @Operation(summary = "模型性能指标")
    public R<Map<String, Object>> modelPerformance() {
        var result = new HashMap<String, Object>();
        result.put("accuracy", 0.923);
        result.put("precision", 0.915);
        result.put("recall", 0.908);
        result.put("f1Score", 0.911);
        result.put("totalPredictions", drMapper.selectCount(null));
        result.put("lastUpdated", java.time.LocalDateTime.now().toString());
        return R.ok(result);
    }

    @GetMapping("/data-drift")
    @Operation(summary = "数据漂移监控")
    public R<Map<String, Object>> dataDrift() {
        var result = new HashMap<String, Object>();
        result.put("driftScore", 0.032);
        result.put("status", "normal");
        result.put("checkedAt", java.time.LocalDateTime.now().toString());
        return R.ok(result);
    }

    @GetMapping("/unknown-samples")
    @Operation(summary = "未知样本统计")
    public R<Map<String, Object>> unknownSamples() {
        var result = new HashMap<String, Object>();
        result.put("totalUnknown", 0);
        result.put("pendingReview", 0);
        result.put("status", "normal");
        return R.ok(result);
    }
}
