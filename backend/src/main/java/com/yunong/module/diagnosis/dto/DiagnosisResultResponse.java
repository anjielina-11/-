package com.yunong.module.diagnosis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisResultResponse {

    private String status;
    private String diseaseName;
    private BigDecimal confidence;
    private String treatment;
    private List<Citation> citations;
    private Map<String, Object> contextSummary;
    private List<Map<String, Object>> agentTrace;

    public DiagnosisResultResponse(String status, String diseaseName, BigDecimal confidence,
                                   String treatment, List<Citation> citations) {
        this(status, diseaseName, confidence, treatment, citations, Map.of(), List.of());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation {
        private String docTitle;
        private String snippet;
    }
}
