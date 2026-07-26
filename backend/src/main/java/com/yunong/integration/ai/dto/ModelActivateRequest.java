package com.yunong.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ModelActivateRequest(
        @JsonProperty("model_id") String modelId,
        @JsonProperty("model_name") String modelName,
        String version,
        @JsonProperty("model_path") String modelPath,
        @JsonProperty("class_to_idx_path") String classToIdxPath,
        @JsonProperty("num_classes") Integer numClasses,
        @JsonProperty("confidence_threshold") Double confidenceThreshold
) {
}