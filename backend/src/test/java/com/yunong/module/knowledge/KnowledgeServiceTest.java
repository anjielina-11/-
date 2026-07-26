package com.yunong.module.knowledge;

import com.yunong.exception.BusinessException;
import com.yunong.integration.ai.AiServiceClient;
import com.yunong.integration.ai.dto.KnowledgeSyncRequest;
import com.yunong.module.knowledge.entity.KnowledgeDocument;
import com.yunong.module.knowledge.mapper.KnowledgeDocumentMapper;
import com.yunong.module.knowledge.service.KnowledgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeServiceTest {

    private KnowledgeDocumentMapper mapper;
    private AiServiceClient aiServiceClient;
    private KnowledgeService service;

    @BeforeEach
    void setUp() {
        mapper = mock(KnowledgeDocumentMapper.class);
        aiServiceClient = mock(AiServiceClient.class);
        service = new KnowledgeService(mapper, aiServiceClient);
    }

    @Test
    void createsDraftByDefaultWithoutSynchronizing() {
        var document = document("k1", null, 0, "初稿内容");

        var result = service.create(document, "admin-1");

        assertThat(result.getStatus()).isEqualTo("draft");
        assertThat(result.getVersion()).isEqualTo(1);
        assertThat(result.getAuthorId()).isEqualTo("admin-1");
        verify(mapper).insert(document);
        verify(aiServiceClient, never()).replaceKnowledge(any());
    }

    @Test
    void publishingSynchronizesAllPublishedDocumentsAndIncrementsContentVersion() {
        var stored = document("k1", "draft", 1, "旧内容");
        var anotherPublished = document("k2", "published", 3, "水稻稻瘟病防治");
        when(mapper.selectById("k1")).thenReturn(stored);
        when(mapper.selectList(any())).thenAnswer(invocation -> List.of(stored, anotherPublished));
        var update = new KnowledgeDocument();
        update.setStatus("published");
        update.setContent("新内容");

        var result = service.update("k1", update);

        assertThat(result.getVersion()).isEqualTo(2);
        var request = captureSyncRequest();
        assertThat(request.documents()).extracting(KnowledgeSyncRequest.Document::status)
                .containsOnly("published");
        assertThat(request.documents()).extracting(KnowledgeSyncRequest.Document::id)
                .containsExactly("k1", "k2");
    }

    @Test
    void archiveRemovesDocumentFromPublishedSynchronization() {
        var stored = document("k1", "published", 2, "即将归档");
        var remaining = document("k2", "published", 1, "继续发布");
        when(mapper.selectById("k1")).thenReturn(stored);
        when(mapper.selectList(any())).thenReturn(List.of(remaining));

        var result = service.archive("k1");

        assertThat(result.getStatus()).isEqualTo("archived");
        assertThat(result.getVersion()).isEqualTo(3);
        var request = captureSyncRequest();
        assertThat(request.documents()).extracting(KnowledgeSyncRequest.Document::id)
                .containsExactly("k2");
    }

    @Test
    void rejectsUnknownStatus() {
        var document = document("k1", "deleted", 1, "内容");

        assertThatThrownBy(() -> service.create(document, "admin"))
                .isInstanceOf(BusinessException.class);
        verify(mapper, never()).insert(any(KnowledgeDocument.class));
    }

    @Test
    void propagatesSynchronizationFailureForTransactionalRollback() {
        var stored = document("k1", "draft", 1, "旧内容");
        when(mapper.selectById("k1")).thenReturn(stored);
        when(mapper.selectList(any())).thenReturn(List.of(stored));
        doThrow(new BusinessException(500, "AI unavailable"))
                .when(aiServiceClient).replaceKnowledge(any());
        var update = new KnowledgeDocument();
        update.setStatus("published");

        assertThatThrownBy(() -> service.update("k1", update))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI unavailable");
        verify(mapper).updateById(stored);
    }

    private KnowledgeSyncRequest captureSyncRequest() {
        var captor = ArgumentCaptor.forClass(KnowledgeSyncRequest.class);
        verify(aiServiceClient).replaceKnowledge(captor.capture());
        return captor.getValue();
    }

    private KnowledgeDocument document(String id, String status, int version, String content) {
        var document = new KnowledgeDocument();
        document.setId(id);
        document.setTitle("知识-" + id);
        document.setCategory("disease");
        document.setTags("[\"水稻\",\"病害\"]");
        document.setStatus(status);
        document.setVersion(version);
        document.setContent(content);
        return document;
    }
}