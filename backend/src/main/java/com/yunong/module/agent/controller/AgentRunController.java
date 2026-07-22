package com.yunong.module.agent.controller;

import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.module.agent.entity.AgentRun;
import com.yunong.module.agent.service.AgentRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/agent-runs")
@RequiredArgsConstructor
@Tag(name = "Agent 运行记录", description = "Agent 推理日志查询和追踪")
public class AgentRunController {

    private final AgentRunService service;

    @GetMapping
    @Operation(summary = "Agent 运行记录列表")
    public R<PageResult<AgentRun>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String diagnosisId,
            @RequestParam(required = false) String status) {
        return R.ok(service.list(page, size, diagnosisId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Agent 运行详情")
    public R<AgentRun> getById(@PathVariable String id) {
        return R.ok(service.getById(id));
    }
}
