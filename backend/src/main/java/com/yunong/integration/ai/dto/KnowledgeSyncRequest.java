package com.yunong.integration.ai.dto;

import java.util.List;

public record KnowledgeSyncRequest(List<Document> documents) {

    public KnowledgeSyncRequest {
        documents = documents == null ? List.of() : List.copyOf(documents);
    }

    public record Document(
            String id,
            String title,
            String category,
            Integer version,
            String content,
            List<String> tags,
            String status
    ) {
        public Document {
            tags = tags == null ? List.of() : List.copyOf(tags);
        }
    }
}