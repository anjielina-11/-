package com.yunong.module.diagnosis;

import com.yunong.module.agent.service.AgentRunService;
import com.yunong.module.diagnosis.mapper.DiagnosisRecordMapper;
import com.yunong.module.diagnosis.service.AsyncDiagnosisService;
import com.yunong.module.review.service.ReviewQueueService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import com.yunong.config.MinioConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AsyncDiagnosisServiceTest {

    @Test
    void forwardsMinioObjectAsMultipartAndUsesAdviceEndpoint() throws Exception {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        var minio = mock(MinioClient.class);
        when(minio.getObject(any(GetObjectArgs.class))).thenReturn(
                new io.minio.GetObjectResponse(null, "bucket", "region", "diagnosis/test.jpg",
                        new ByteArrayInputStream("image-bytes".getBytes())));
        var minioConfig = new MinioConfig();
        minioConfig.setBucket("yunnong-images");
        var service = new AsyncDiagnosisService(
                mock(DiagnosisRecordMapper.class), mock(AgentRunService.class),
                mock(ReviewQueueService.class), restTemplate, minio,
                minioConfig, "http://ai:8000");

        server.expect(requestTo("http://ai:8000/api/v1/diagnosis/simple?crop_info=%E6%9C%AA%E7%9F%A5%E4%BD%9C%E7%89%A9"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andRespond(withSuccess("{\"disease_name\":\"rice_blast\",\"confidence\":0.9}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://ai:8000/api/v1/diagnosis/advice"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess("{\"advice\":\"建议文本\"}", MediaType.APPLICATION_JSON));

        service.callImageDiagnosis("diagnosis/test.jpg");
        service.callAgentGenerate("rice_blast", new java.math.BigDecimal("0.9"), java.util.List.of());

        server.verify();
    }
}
