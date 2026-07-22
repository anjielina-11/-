package com.yunong.module.diagnosis;

import com.yunong.module.diagnosis.dto.DiagnosisResultResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("诊断结果响应测试")
class DiagnosisResultResponseTest {

    @Test
    @DisplayName("完整结果包含所有字段")
    void fullResult() {
        var citation = new DiagnosisResultResponse.Citation("水稻病虫害防治规范", "抽穗期遇阴雨天气...");
        var result = new DiagnosisResultResponse(
                "completed", "稻瘟病", new BigDecimal("0.92"),
                "防治建议文字", List.of(citation)
        );

        assertThat(result.getStatus()).isEqualTo("completed");
        assertThat(result.getDiseaseName()).isEqualTo("稻瘟病");
        assertThat(result.getConfidence()).isEqualTo(new BigDecimal("0.92"));
        assertThat(result.getCitations()).hasSize(1);
        assertThat(result.getCitations().get(0).getDocTitle()).isEqualTo("水稻病虫害防治规范");
    }

    @Test
    @DisplayName("空结果无引用")
    void emptyCitations() {
        var result = new DiagnosisResultResponse();
        result.setStatus("processing");
        result.setCitations(List.of());
        assertThat(result.getCitations()).isEmpty();
    }
}
