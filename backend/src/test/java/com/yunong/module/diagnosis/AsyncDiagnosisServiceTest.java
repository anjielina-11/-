package com.yunong.module.diagnosis;

import cn.hutool.json.JSONUtil;
import com.yunong.config.MinioConfig;
import com.yunong.integration.ai.AiServiceClient;
import com.yunong.module.agent.entity.AgentRun;
import com.yunong.module.agent.service.AgentRunService;
import com.yunong.module.diagnosis.dto.DiagnosisContext;
import com.yunong.module.diagnosis.entity.DiagnosisRecord;
import com.yunong.module.diagnosis.mapper.DiagnosisRecordMapper;
import com.yunong.module.diagnosis.service.AsyncDiagnosisService;
import com.yunong.module.diagnosis.service.DiagnosisContextService;
import com.yunong.module.review.service.ReviewQueueService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AsyncDiagnosisServiceTest {

    @Test
    void processesDiagnosisWithStructuredContextAndPersistsAgentTrace() throws Exception {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        var minio = mock(MinioClient.class);
        when(minio.getObject(any(GetObjectArgs.class))).thenReturn(
                new io.minio.GetObjectResponse(null, "bucket", "region", "diagnosis/test.jpg",
                        new ByteArrayInputStream("image-bytes".getBytes())));
        var minioConfig = new MinioConfig();
        minioConfig.setBucket("yunnong-images");

        var diagnosisMapper = mock(DiagnosisRecordMapper.class);
        var agentRunService = mock(AgentRunService.class);
        var contextService = mock(DiagnosisContextService.class);
        var diagnosis = new DiagnosisRecord();
        diagnosis.setId("diagnosis-1");
        when(diagnosisMapper.selectById("diagnosis-1")).thenReturn(diagnosis);
        var run = new AgentRun();
        run.setId("run-1");
        when(agentRunService.start(eq("diagnosis-1"), any(), any(), any())).thenReturn(run);

        var context = new DiagnosisContext(
                new DiagnosisContext.CropContext("水稻", "滇粳验收", LocalDate.of(2026, 7, 1), "tillering"),
                new DiagnosisContext.FieldContext("A-01", "验收农场"),
                List.of(new DiagnosisContext.WeatherForecast(LocalDate.of(2026, 7, 26), "阵雨",
                        new BigDecimal("28"), new BigDecimal("86"), new BigDecimal("8.5"), BigDecimal.ONE))
        );
        when(contextService.load("diagnosis-1")).thenReturn(context);

        var aiClient = new AiServiceClient(restTemplate, "http://ai:8000");
        var service = new AsyncDiagnosisService(
                diagnosisMapper, agentRunService, mock(ReviewQueueService.class), contextService, aiClient,
                restTemplate, minio, minioConfig, "http://ai:8000");

        server.expect(requestTo("http://ai:8000/api/v1/diagnosis/simple?crop_info=%E6%9C%AA%E7%9F%A5%E4%BD%9C%E7%89%A9"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andRespond(withSuccess("{\"disease_name\":\"rice_blast\",\"confidence\":0.9}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://ai:8000/api/v1/rag/retrieve"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.query").value("rice_blast 防治方法"))
                .andExpect(jsonPath("$.top_k").value(3))
                .andRespond(withSuccess("{\"results\":[{\"docTitle\":\"稻瘟病防治\",\"snippet\":\"及时用药\"}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://ai:8000/api/v1/diagnosis/advice"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.crop.name").value("水稻"))
                .andExpect(jsonPath("$.crop.planting_date").value("2026-07-01"))
                .andExpect(jsonPath("$.crop.growth_stage").value("tillering"))
                .andExpect(jsonPath("$.field.farm_name").value("验收农场"))
                .andExpect(jsonPath("$.weather_forecast[0].date").value("2026-07-26"))
                .andExpect(jsonPath("$.weather_forecast[0].humidity").value(86))
                .andExpect(jsonPath("$.citations[0].docTitle").value("稻瘟病防治"))
                .andRespond(withSuccess("{\"advice\":\"建议文本\",\"references\":[{\"docTitle\":\"稻瘟病防治\",\"snippet\":\"及时用药\"}],\"context_summary\":{\"crop_name\":\"水稻\"},\"agent_trace\":[{\"agent\":\"weather-risk\",\"status\":\"completed\",\"summary\":\"高湿\"}]}", MediaType.APPLICATION_JSON));

        service.processAsync("diagnosis-1", "diagnosis/test.jpg");

        assertThat(diagnosis.getDiseaseName()).isEqualTo("rice_blast");
        assertThat(diagnosis.getReviewStatus()).isEqualTo("pending");
        var savedResult = JSONUtil.parseObj(diagnosis.getAiResult());
        assertThat(savedResult.getStr("treatment")).isEqualTo("建议文本");
        assertThat(savedResult.getJSONObject("contextSummary").getStr("crop_name")).isEqualTo("水稻");
        assertThat(savedResult.getJSONArray("agentTrace").getJSONObject(0).getStr("agent")).isEqualTo("weather-risk");
        assertThat(savedResult.getJSONArray("citations").getJSONObject(0).getStr("docTitle")).isEqualTo("稻瘟病防治");

        var outputCaptor = ArgumentCaptor.forClass(String.class);
        verify(agentRunService).complete(eq("run-1"), outputCaptor.capture(), eq(150), eq(new BigDecimal("0.003")));
        assertThat(JSONUtil.parseObj(outputCaptor.getValue()).getJSONArray("agentTrace")).hasSize(1);
        verify(diagnosisMapper).updateById(diagnosis);
        server.verify();
    }

    @Test
    void preservesRejectedModelConfidenceForTechnicianReview() throws Exception {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        var minio = mock(MinioClient.class);
        when(minio.getObject(any(GetObjectArgs.class))).thenReturn(
                new io.minio.GetObjectResponse(null, "bucket", "region", "diagnosis/leaf.png",
                        new ByteArrayInputStream("image-bytes".getBytes())));
        var minioConfig = new MinioConfig();
        minioConfig.setBucket("yunnong-images");

        var diagnosisMapper = mock(DiagnosisRecordMapper.class);
        var agentRunService = mock(AgentRunService.class);
        var diagnosis = new DiagnosisRecord();
        diagnosis.setId("diagnosis-low-confidence");
        when(diagnosisMapper.selectById("diagnosis-low-confidence")).thenReturn(diagnosis);
        var run = new AgentRun();
        run.setId("run-low-confidence");
        when(agentRunService.start(eq("diagnosis-low-confidence"), any(), any(), any())).thenReturn(run);

        var aiClient = new AiServiceClient(restTemplate, "http://ai:8000");
        var service = new AsyncDiagnosisService(
                diagnosisMapper, agentRunService, mock(ReviewQueueService.class),
                mock(DiagnosisContextService.class), aiClient, restTemplate, minio, minioConfig,
                "http://ai:8000");

        server.expect(requestTo("http://ai:8000/api/v1/diagnosis/simple?crop_info=%E6%9C%AA%E7%9F%A5%E4%BD%9C%E7%89%A9"))
                .andRespond(withSuccess("{\"disease_name\":\"未知病害\",\"confidence\":0.176073,\"description\":\"最高候选 potato_late_blight，置信度 0.1761 低于阈值 0.6000\"}", MediaType.APPLICATION_JSON));

        service.processAsync("diagnosis-low-confidence", "diagnosis/leaf.png");

        assertThat(diagnosis.getDiseaseName()).isEqualTo("未知病害");
        assertThat(diagnosis.getConfidence()).isEqualByComparingTo("0.176073");
        assertThat(diagnosis.getReviewStatus()).isEqualTo("pending_review");
        var savedResult = JSONUtil.parseObj(diagnosis.getAiResult());
        assertThat(savedResult.getBigDecimal("confidence")).isEqualByComparingTo("0.176073");
        assertThat(savedResult.getStr("treatment")).contains("17.61%");
        server.verify();
    }

}
