package com.resumego.knowledge;

import com.resumego.knowledge.dto.CreateKnowledgeDocumentRequest;
import com.resumego.knowledge.dto.KnowledgeDocumentResponse;
import com.resumego.knowledge.dto.UpdateKnowledgeDocumentTitleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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
    void createsNoteWithEmptyContentAndCompletedStatus() {
        when(repository.createNoteWithEmptyContent(1L, "TensorFlow 学习笔记")).thenReturn(10L);
        when(repository.findById(1L, 10L)).thenReturn(Optional.of(new KnowledgeDocument(10L, 1L, "TensorFlow 学习笔记", "NOTE", "COMPLETED",
                LocalDateTime.now(), LocalDateTime.now())));
        KnowledgeDocumentResponse response = service.create(new CreateKnowledgeDocumentRequest(
                "  TensorFlow  学习笔记 ", "NOTE"));
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.sourceType()).isEqualTo("NOTE");
        assertThat(response.processingStatus()).isEqualTo("COMPLETED");
        assertThat(response.sourceExtension()).isNull();
        verify(repository).createNoteWithEmptyContent(1L, "TensorFlow 学习笔记");
    }

    @Test
    void updatesOwnedTitleAndRejectsBlankOrForeign() {
        when(repository.updateDocumentTitle(1L, 5L, "新标题")).thenReturn(true);
        when(repository.findById(1L, 5L)).thenReturn(Optional.of(new KnowledgeDocument(5L, 1L, "新标题", "NOTE", "COMPLETED",
                LocalDateTime.now(), LocalDateTime.now())));
        KnowledgeDocumentResponse response = service.updateTitle(5L, new UpdateKnowledgeDocumentTitleRequest("  新标题  "));
        assertThat(response.title()).isEqualTo("新标题");

        // 空白标题拒绝
        assertThatThrownBy(() -> service.updateTitle(5L, new UpdateKnowledgeDocumentTitleRequest("   ")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("标题");
        // 跨用户/缺失：updateDocumentTitle 返回 false → 404
        when(repository.updateDocumentTitle(1L, 404L, "x")).thenReturn(false);
        assertThatThrownBy(() -> service.updateTitle(404L, new UpdateKnowledgeDocumentTitleRequest("x")))
                .isInstanceOf(java.util.NoSuchElementException.class).hasMessageContaining("不存在");
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
        when(repository.listByUserFiltered(1L, null, null)).thenReturn(List.of(doc));
        when(repository.findById(1L, 1L)).thenReturn(Optional.of(doc));

        assertThat(service.list()).hasSize(1);
        assertThat(service.get(1L).id()).isEqualTo(1L);
    }

    @Test
    void exposesOriginalFileNameSoClientsCanShowTheRealFileType() {
        KnowledgeDocument doc = new KnowledgeDocument(2L, 1L, "Redis 笔记", "FILE", "COMPLETED",
                LocalDateTime.now(), LocalDateTime.now());
        KnowledgeSourceFile source = new KnowledgeSourceFile(20L, 2L, 1L, "redis-notes.md",
                "sources/1/hash.md", "text/markdown", "md", 12L, "hash", "AVAILABLE", null,
                LocalDateTime.now(), LocalDateTime.now());
        when(repository.findById(1L, 2L)).thenReturn(Optional.of(doc));
        when(repository.findSourceFileByDocument(1L, 2L)).thenReturn(Optional.of(source));

        assertThat(service.get(2L).sourceFile()).isEqualTo("redis-notes.md");
        assertThat(service.get(2L).sizeBytes()).isEqualTo(12L);
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

    @Test
    void savesNoteContentFirstTimeAndOverwrites() {
        KnowledgeDocument doc = new KnowledgeDocument(5L, 1L, "笔记", "NOTE", "NOT_STARTED",
                LocalDateTime.now(), LocalDateTime.now());
        when(repository.findById(1L, 5L)).thenReturn(Optional.of(doc));

        assertThat(service.saveNoteContent(5L, "正文内容").content()).isEqualTo("正文内容");
        verify(repository).saveNoteContent(5L, 1L, "正文内容");

        // 覆盖保存
        assertThat(service.saveNoteContent(5L, "新正文").content()).isEqualTo("新正文");
        verify(repository).saveNoteContent(5L, 1L, "新正文");
    }

    @Test
    void noteContentAllowsEmptyString() {
        KnowledgeDocument doc = new KnowledgeDocument(5L, 1L, "笔记", "NOTE", "COMPLETED",
                LocalDateTime.now(), LocalDateTime.now());
        when(repository.findById(1L, 5L)).thenReturn(Optional.of(doc));
        assertThat(service.saveNoteContent(5L, "").content()).isEqualTo("");
        verify(repository).saveNoteContent(5L, 1L, "");
    }

    @Test
    void noteContentEnforcesOneMibUtf8ByteLimit() {
        KnowledgeDocument doc = new KnowledgeDocument(5L, 1L, "笔记", "NOTE", "NOT_STARTED",
                LocalDateTime.now(), LocalDateTime.now());
        when(repository.findById(1L, 5L)).thenReturn(Optional.of(doc));
        String max = "a".repeat(1024 * 1024);
        assertThat(service.saveNoteContent(5L, max).content()).hasSize(1024 * 1024);
        assertThatThrownBy(() -> service.saveNoteContent(5L, "a".repeat(1024 * 1024 + 1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("1 MiB");
        verify(repository, times(1)).saveNoteContent(5L, 1L, max);
    }

    @Test
    void noteContentRejectsFileAndMissingDocuments() {
        KnowledgeDocument file = new KnowledgeDocument(6L, 1L, "文件", "FILE", "COMPLETED",
                LocalDateTime.now(), LocalDateTime.now());
        when(repository.findById(1L, 6L)).thenReturn(Optional.of(file));
        assertThatThrownBy(() -> service.saveNoteContent(6L, "x"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("NOTE");

        when(repository.findById(1L, 404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.saveNoteContent(404L, "x"))
                .isInstanceOf(java.util.NoSuchElementException.class).hasMessageContaining("不存在");
        verify(repository, never()).saveNoteContent(anyLong(), anyLong(), any());
    }
}
