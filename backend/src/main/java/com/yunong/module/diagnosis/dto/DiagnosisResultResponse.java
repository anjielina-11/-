package com.yunong.module.diagnosis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisResultResponse {

    private String status;
    private String diseaseName;
    private BigDecimal confidence;
    private String treatment;
    private List<Citation> citations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation {
        private String docTitle;
        private String snippet;
    }
}
