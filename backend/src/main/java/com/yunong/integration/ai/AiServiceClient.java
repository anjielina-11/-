package com.yunong.integration.ai;

import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.integration.ai.dto.AgentAdviceRequest;
import com.yunong.integration.ai.dto.AgentAdviceResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class AiServiceClient {

    private final RestTemplate restTemplate;
    private final String aiServiceUrl;

    public AiServiceClient(RestTemplate restTemplate, @Qualifier("aiServiceUrl") String aiServiceUrl) {
        this.restTemplate = restTemplate;
        this.aiServiceUrl = aiServiceUrl;
    }

    public AgentAdviceResponse generateAdvice(AgentAdviceRequest request) {
        try {
            var response = restTemplate.postForEntity(
                    aiServiceUrl + "/api/v1/diagnosis/advice", request, AgentAdviceResponse.class);
            if (response.getBody() == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 建议响应为空");
            }
            return response.getBody();
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 建议服务调用失败: " + exception.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> retrieveKnowledge(String query, int topK) {
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiServiceUrl + "/api/v1/rag/retrieve",
                    Map.of("query", query, "top_k", topK), Map.class);
            if (response.getBody() == null) {
                return List.of();
            }
            Object results = response.getBody().get("results");
            return results instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "RAG 检索服务调用失败: " + exception.getMessage());
        }
    }
}