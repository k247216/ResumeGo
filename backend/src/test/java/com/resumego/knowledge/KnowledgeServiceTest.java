package com.resumego.knowledge;

import com.resumego.knowledge.dto.CreateKnowledgeDocumentRequest;
import com.resumego.knowledge.dto.KnowledgeDocumentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class KnowledgeServiceTest {

    private KnowledgeRepository repository;
    private KnowledgeService service;

    @BeforeEach
    void setUp() {
        repository = mock(KnowledgeRepository.class);
        service = new KnowledgeService(repository);
    }

    @Test
    void createsNoteWithNormalizedTitleAndNotStartedStatus() {
        when(repository.insertDocument(1L, "TensorFlow 学习笔记", "NOTE", "NOT_STARTED")).thenReturn(10L);
        when(repository.findById(1L, 10L)).thenReturn(Optional.of(new KnowledgeDocument(10L, 1L, "TensorFlow 学习笔记", "NOTE", "NOT_STARTED",
                LocalDateTime.now(), LocalDateTime.now())));
        KnowledgeDocumentResponse response = service.create(new CreateKnowledgeDocumentRequest(
                "  TensorFlow  学习笔记 ", "NOTE"));
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.sourceType()).isEqualTo("NOTE");
        assertThat(response.processingStatus()).isEqualTo("NOT_STARTED");
        assertThat(response.sourceFile()).isNull();
        verify(repository).insertDocument(1L, "TensorFlow 学习笔记", "NOTE", "NOT_STARTED");
    }

    @Test
    void rejectsBlankOrOverlongTitle() {
        assertThatThrownBy(() -> service.create(new CreateKnowledgeDocumentRequest("   ", "NOTE")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("标题");
        assertThatThrownBy(() -> service.create(new CreateKnowledgeDocumentRequest("a".repeat(121), "NOTE")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).insertDocument(anyLong(), any(), any(), any());
    }

    @Test
    void rejectsFileSourceTypeInThisSlice() {
        assertThatThrownBy(() -> service.create(new CreateKnowledgeDocumentRequest("有效标题", "FILE")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("FILE");
        verify(repository, never()).insertDocument(anyLong(), any(), any(), any());
    }

    @Test
    void listsAndGetsOwnedDocuments() {
        KnowledgeDocument doc = new KnowledgeDocument(1L, 1L, "笔记", "NOTE", "NOT_STARTED",
                LocalDateTime.now(), LocalDateTime.now());
        when(repository.listByUser(1L)).thenReturn(List.of(doc));
        when(repository.findById(1L, 1L)).thenReturn(Optional.of(doc));

        assertThat(service.list()).hasSize(1);
        assertThat(service.get(1L).id()).isEqualTo(1L);
    }

    @Test
    void rejectsMissingOrForeignDocumentWith404() {
        when(repository.findById(1L, 404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(404L))
                .isInstanceOf(java.util.NoSuchElementException.class).hasMessageContaining("不存在");
    }

    @Test
    void returnsExtractedContentForCompletedDocument() {
        KnowledgeDocument doc = new KnowledgeDocument(5L, 1L, "本地文件知识", "FILE", "COMPLETED",
                LocalDateTime.now(), LocalDateTime.now());
        when(repository.findById(1L, 5L)).thenReturn(Optional.of(doc));
        when(repository.findExtractedContentByDocument(1L, 5L))
                .thenReturn(Optional.of(new KnowledgeExtractedContent(1L, 5L, 1L, "提取正文",
                        LocalDateTime.now(), LocalDateTime.now())));

        assertThat(service.getContent(5L).content()).isEqualTo("提取正文");
    }

    @Test
    void contentRejectsDocumentThatIsNotCompleted() {
        KnowledgeDocument doc = new KnowledgeDocument(5L, 1L, "本地文件知识", "FILE", "PENDING",
                LocalDateTime.now(), LocalDateTime.now());
        when(repository.findById(1L, 5L)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.getContent(5L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("尚未完成");
    }

    @Test
    void contentRejectsMissingOrForeignDocument() {
        when(repository.findById(1L, 404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getContent(404L))
                .isInstanceOf(java.util.NoSuchElementException.class).hasMessageContaining("不存在");
    }
}
