package com.yunong.module.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.agent.entity.AgentRun;
import com.yunong.module.agent.mapper.AgentRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AgentRunService {

    private final AgentRunMapper mapper;

    public AgentRun start(String diagnosisId, String agentType, String agentName, String inputJson) {
        var run = new AgentRun();
        run.setDiagnosisId(diagnosisId);
        run.setAgentType(agentType);
        run.setAgentName(agentName);
        run.setInputJson(inputJson);
        run.setStatus("running");
        run.setStartedAt(LocalDateTime.now());
        mapper.insert(run);
        return run;
    }

    public void complete(String id, String outputJson, Integer tokensUsed, java.math.BigDecimal cost) {
        var run = mapper.selectById(id);
        if (run == null) throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        run.setStatus("completed");
        run.setOutputJson(outputJson);
        run.setTokensUsed(tokensUsed);
        run.setCost(cost);
        run.setCompletedAt(LocalDateTime.now());
        mapper.updateById(run);
    }

    public void fail(String id, String errorMessage) {
        var run = mapper.selectById(id);
        if (run != null) {
            run.setStatus("failed");
            run.setErrorMessage(errorMessage);
            run.setCompletedAt(LocalDateTime.now());
            mapper.updateById(run);
        }
    }

    public PageResult<AgentRun> list(int page, int size, String diagnosisId, String status) {
        var wrapper = new LambdaQueryWrapper<AgentRun>();
        if (diagnosisId != null) wrapper.eq(AgentRun::getDiagnosisId, diagnosisId);
        if (status != null) wrapper.eq(AgentRun::getStatus, status);
        wrapper.orderByDesc(AgentRun::getCreatedAt);
        var result = mapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public AgentRun getById(String id) {
        return mapper.selectById(id);
    }
}
