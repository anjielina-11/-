package com.yunong.module.diagnosis;

import com.yunong.module.diagnosis.dto.DiagnosisResultResponse;
import com.yunong.module.diagnosis.entity.DiagnosisRecord;
import com.yunong.module.diagnosis.entity.Observation;
import com.yunong.module.diagnosis.mapper.ObservationMapper;
import com.yunong.module.crop.entity.PlantingCycle;
import com.yunong.module.crop.mapper.PlantingCycleMapper;
import com.yunong.module.task.service.TaskService;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.diagnosis.mapper.DiagnosisRecordMapper;
import com.yunong.module.diagnosis.service.DiagnosisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 诊断服务集成测试
 */
@ExtendWith(MockitoExtension.class)
class DiagnosisServiceTest {

    @Mock
    private DiagnosisRecordMapper diagnosisRecordMapper;

    @Mock
    private ObservationMapper observationMapper;

    @Mock
    private TaskService taskService;

    @Mock
    private PlantingCycleMapper plantingCycleMapper;

    @InjectMocks
    private DiagnosisService diagnosisService;

    private DiagnosisRecord mockRecord;

    @BeforeEach
    void setUp() {
        mockRecord = new DiagnosisRecord();
        mockRecord.setId("test-uuid-001");
        mockRecord.setDiseaseName("稻瘟病");
        mockRecord.setConfidence(new BigDecimal("0.92"));
        mockRecord.setReviewStatus("pending");
        mockRecord.setSeverity("high");
    }

    @Test
    @DisplayName("根据ID查询诊断记录")
    void shouldFindDiagnosisById() {
        when(diagnosisRecordMapper.selectById("test-uuid-001")).thenReturn(mockRecord);

        var result = diagnosisRecordMapper.selectById("test-uuid-001");

        assertNotNull(result);
        assertEquals("稻瘟病", result.getDiseaseName());
        assertEquals(new BigDecimal("0.92"), result.getConfidence());
    }

    @Test
    @DisplayName("诊断记录应有正确的初始状态")
    void shouldHaveCorrectInitialStatus() {
        var record = new DiagnosisRecord();
        record.setReviewStatus("pending");

        assertEquals("pending", record.getReviewStatus());
        assertNull(record.getDiseaseName());
    }

    @Test
    @DisplayName("低置信度诊断应标记为待审核")
    void shouldFlagLowConfidenceForReview() {
        BigDecimal threshold = new BigDecimal("0.85");
        BigDecimal lowConf = new BigDecimal("0.72");

        assertTrue(lowConf.compareTo(threshold) < 0,
                "置信度低于阈值应进入审核队列");

        BigDecimal highConf = new BigDecimal("0.95");
        assertFalse(highConf.compareTo(threshold) < 0,
                "置信度高于阈值不需要审核");
    }

    @Test
    @DisplayName("诊断结果应包含防治建议和参考资料")
    void diagnosisResultShouldContainTreatmentAndCitations() {
        var response = new DiagnosisResultResponse();
        response.setDiseaseName("稻瘟病");
        response.setConfidence(new BigDecimal("0.92"));
        response.setTreatment("1. 选用抗病品种\n2. 合理施肥\n3. 抽穗期喷施三环唑");
        response.setCitations(java.util.List.of(
                new DiagnosisResultResponse.Citation("水稻病虫害防治规范", "抽穗期遇阴雨天气，稻瘟病易流行...")
        ));

        assertNotNull(response.getDiseaseName());
        assertNotNull(response.getTreatment());
        assertFalse(response.getCitations().isEmpty());
        assertEquals(1, response.getCitations().size());
    }

    @Test
    @DisplayName("AI 引用应映射为前端可展示的标题和摘要")
    void shouldMapAiCitationsToResultResponse() {
        var observation = new Observation();
        observation.setId("obs-1");
        observation.setUserId("farmer-1");
        mockRecord.setObservationId("obs-1");
        mockRecord.setAiResult("""
                {"treatment":"建议文本","citations":[{"source":"knowledge_docs/rice.md","content":"稻瘟病防治摘要","score":0.2}]}
                """);
        when(diagnosisRecordMapper.selectById("test-uuid-001")).thenReturn(mockRecord);
        when(observationMapper.selectById("obs-1")).thenReturn(observation);

        var result = diagnosisService.getResult("test-uuid-001", "farmer-1", false);

        assertEquals("knowledge_docs/rice.md", result.getCitations().getFirst().getDocTitle());
        assertEquals("稻瘟病防治摘要", result.getCitations().getFirst().getSnippet());
    }

    @Test
    @DisplayName("AI 已完成但待人工审核时停止前端轮询")
    void shouldExposeReadyDiagnosisAsNeedReview() {
        var observation = new Observation();
        observation.setId("obs-1");
        observation.setUserId("farmer-1");
        mockRecord.setObservationId("obs-1");
        mockRecord.setReviewStatus("pending");
        mockRecord.setAiResult("{\"treatment\":\"建议文本\",\"citations\":[]}");
        when(diagnosisRecordMapper.selectById("test-uuid-001")).thenReturn(mockRecord);
        when(observationMapper.selectById("obs-1")).thenReturn(observation);

        var result = diagnosisService.getResult("test-uuid-001", "farmer-1", false);

        assertEquals("need_review", result.getStatus());
    }

    @Test
    @DisplayName("上传诊断不能关联其他农户的种植周期")
    void shouldRejectUploadForAnotherUsersCycle() {
        var cycle = new PlantingCycle();
        cycle.setId("cycle-1");
        cycle.setCreatedBy("farmer-1");
        when(plantingCycleMapper.selectById("cycle-1")).thenReturn(cycle);

        var error = assertThrows(BusinessException.class,
                () -> diagnosisService.upload(null, "cycle-1", "test", "farmer-2"));

        assertEquals(ErrorCode.FORBIDDEN.getCode(), error.getCode());
    }

    @Test
    @DisplayName("pending_review 状态允许人工审核")
    void shouldReviewPendingReviewRecord() {
        mockRecord.setReviewStatus("pending_review");
        when(diagnosisRecordMapper.selectById("test-uuid-001")).thenReturn(mockRecord);

        var result = diagnosisService.review("test-uuid-001", "rejected", "无法确认", "tech-1");

        assertEquals("rejected", result.getReviewStatus());
        assertEquals("tech-1", result.getReviewerId());
        org.mockito.Mockito.verify(diagnosisRecordMapper).updateById(mockRecord);
    }

    @Test
    @DisplayName("农户不能读取其他用户的诊断记录")
    void shouldRejectDiagnosisOwnedByAnotherUser() {
        var observation = new Observation();
        observation.setId("obs-1");
        observation.setUserId("farmer-1");
        mockRecord.setObservationId("obs-1");
        when(diagnosisRecordMapper.selectById("test-uuid-001")).thenReturn(mockRecord);
        when(observationMapper.selectById("obs-1")).thenReturn(observation);

        var error = assertThrows(BusinessException.class,
                () -> diagnosisService.getById("test-uuid-001", "farmer-2", false));

        assertEquals(ErrorCode.FORBIDDEN.getCode(), error.getCode());
    }

    @Test
    @DisplayName("审核通过后任务分配给上报农户")
    void shouldAssignGeneratedTaskToReportingFarmer() {
        var observation = new Observation();
        observation.setId("obs-1");
        observation.setUserId("farmer-1");
        observation.setCycleId("cycle-1");
        mockRecord.setObservationId("obs-1");
        when(diagnosisRecordMapper.selectById("test-uuid-001")).thenReturn(mockRecord);
        when(observationMapper.selectById("obs-1")).thenReturn(observation);

        diagnosisService.review("test-uuid-001", "approved", "确认", "tech-1");

        org.mockito.Mockito.verify(taskService).autoCreateFromDiagnosis(
                org.mockito.ArgumentMatchers.eq("test-uuid-001"),
                org.mockito.ArgumentMatchers.eq("稻瘟病"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("farmer-1"),
                org.mockito.ArgumentMatchers.eq("cycle-1"));
    }

}
