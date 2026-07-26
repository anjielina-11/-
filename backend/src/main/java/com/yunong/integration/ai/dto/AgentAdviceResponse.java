package com.yunong.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record AgentAdviceResponse(
        String advice,
        List<Map<String, Object>> references,
        @JsonProperty("context_summary") Map<String, Object> contextSummary,
        @JsonProperty("agent_trace") List<Map<String, Object>> agentTrace
) {
    public AgentAdviceResponse {
        references = references == null ? List.of() : references;
        contextSummary = contextSummary == null ? Map.of() : contextSummary;
        agentTrace = agentTrace == null ? List.of() : agentTrace;
    }
}