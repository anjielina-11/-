package com.yunong.integration.ai;

import com.yunong.module.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiConsistencyInitializer {

    private final KnowledgeService knowledgeService;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void synchronizePublishedKnowledge() {
        try {
            int count = knowledgeService.syncPublished();
            log.info("启动后知识库同步完成: documents={}", count);
        } catch (Exception exception) {
            log.warn("启动后知识库同步失败，将保留当前 AI 知识库: {}", exception.getMessage());
        }
    }
}