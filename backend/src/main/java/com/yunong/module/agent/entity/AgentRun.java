package com.yunong.module.agent.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("agent_runs")
public class AgentRun {

    @TableId
    private String id;
    private String diagnosisId;
    private String agentType;
    private String agentName;
    private String inputJson;
    private String outputJson;
    private String status;
    private String errorMessage;
    private Integer tokensUsed;
    private BigDecimal cost;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
