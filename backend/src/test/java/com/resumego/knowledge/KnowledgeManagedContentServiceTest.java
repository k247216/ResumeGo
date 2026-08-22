package com.resumego.knowledge;

import com.resumego.knowledge.dto.KnowledgeContentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KnowledgeManagedContentServiceTest {

    private KnowledgeRepository repository;
    private KnowledgeFileStore fileStore;
    private PlatformTransactionManager txManager;
    private KnowledgeManagedContentService service;

    @BeforeEach
    void setUp() {
        repository = mock(KnowledgeRepository.class);
        fileStore = mock(KnowledgeFileStore.class);
        txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        service = new KnowledgeManagedContentService(repository, fileStore, txManager);
    }

    private KnowledgeDocument note(long id) {
        return new KnowledgeDocument(id, 1L, "笔记", "NOTE", "COMPLETED", LocalDateTime.now(), LocalDateTime.now());
    }

    private KnowledgeDocument fileDoc(long id) {
        return new KnowledgeDocument(id, 1L, "文件", "FILE", "COMPLETED", LocalDateTime.now(), LocalDateTime.now());
    }

    private KnowledgeSourceFile mdSource(long id, long docId, String sha, String availability) {
        return new KnowledgeSourceFile(id, docId, 1L, "a.md", "knowledge/sources/1/" + sha + ".md",
                null, "md", 10, sha, availability, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void savesNoteContent() {
        when(repository.findById(1L, 1L)).thenReturn(Optional.of(note(1L)));
        KnowledgeContentResponse response = service.saveContent(1L, "笔记正文");
        assertThat(response.content()).isEqualTo("笔记正文");
        verify(repository).saveNoteContent(eq(1L), eq(1L), eq("笔记正文"));
    }

    @Test
    void savesMarkdownManagedCopyAndSyncsMetadata() {
        when(repository.findById(1L, 2L)).thenReturn(Optional.of(fileDoc(2L)));
        when(repository.findSourceFileByDocument(1L, 2L)).thenReturn(Optional.of(mdSource(9L, 2L, "old-sha", "AVAILABLE")));
        when(repository.findSourceFileBySha(eq(1L), any())).thenReturn(Optional.empty());
        when(fileStore.readManagedForReplace(1L, "knowledge/sources/1/old-sha.md")).thenReturn(new byte[]{1});
        when(fileStore.stageReplacement(any())).thenReturn(Path.of("/tmp/staged.part"));

        KnowledgeContentResponse response = service.saveContent(2L, "# 新内容");

        assertThat(response.content()).isEqualTo("# 新内容");
        verify(fileStore).commitReplacement(any(), eq(Path.of("/tmp/staged.part")));
        verify(repository).saveNoteContent(eq(2L), eq(1L), eq("# 新内容"));
        verify(repository).updateSourceFileAfterEdit(eq(9L),
                eq((long) "# 新内容".getBytes(java.nio.charset.StandardCharsets.UTF_8).length), any());
    }

    @Test
    void rejectsTxtAndUnknownExtension() {
        when(repository.findById(1L, 2L)).thenReturn(Optional.of(fileDoc(2L)));
        when(repository.findSourceFileByDocument(1L, 2L)).thenReturn(Optional.of(
                new KnowledgeSourceFile(9L, 2L, 1L, "a.txt", "knowledge/sources/1/a.txt", null, "txt",
                        10, "s", "AVAILABLE", null, LocalDateTime.now(), LocalDateTime.now())));
        assertThatThrownBy(() -> service.saveContent(2L, "x"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("NOT_EDITABLE");
        verify(repository, never()).saveNoteContent(anyLong(), anyLong(), any());
    }

    @Test
    void rejectsUnavailableSource() {
        when(repository.findById(1L, 2L)).thenReturn(Optional.of(fileDoc(2L)));
        when(repository.findSourceFileByDocument(1L, 2L)).thenReturn(Optional.of(mdSource(9L, 2L, "s", "MISSING")));
        assertThatThrownBy(() -> service.saveContent(2L, "x"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("SOURCE_NOT_AVAILABLE");
    }

    @Test
    void rejectsHashConflictWithoutTouchingFiles() {
        when(repository.findById(1L, 2L)).thenReturn(Optional.of(fileDoc(2L)));
        when(repository.findSourceFileByDocument(1L, 2L)).thenReturn(Optional.of(mdSource(9L, 2L, "old", "AVAILABLE")));
        when(repository.findSourceFileBySha(eq(1L), any())).thenReturn(Optional.of(mdSource(99L, 88L, "other", "AVAILABLE")));
        assertThatThrownBy(() -> service.saveContent(2L, "重复内容"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("HASH_CONFLICT");
        verify(fileStore, never()).readManagedForReplace(anyLong(), anyString());
        verify(repository, never()).saveNoteContent(anyLong(), anyLong(), anyString());
    }

    @Test
    void databaseFailureRestoresOldManagedFile() {
        when(repository.findById(1L, 2L)).thenReturn(Optional.of(fileDoc(2L)));
        when(repository.findSourceFileByDocument(1L, 2L)).thenReturn(Optional.of(mdSource(9L, 2L, "old", "AVAILABLE")));
        when(repository.findSourceFileBySha(eq(1L), any())).thenReturn(Optional.empty());
        when(fileStore.readManagedForReplace(1L, "knowledge/sources/1/old.md")).thenReturn(new byte[]{9, 9});
        when(fileStore.stageReplacement(any())).thenReturn(Path.of("/tmp/s.part"));
        doThrow(new RuntimeException("db down")).when(repository).saveNoteContent(2L, 1L, "# 新");

        assertThatThrownBy(() -> service.saveContent(2L, "# 新"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("db down");
        verify(fileStore).restoreManaged(any(), eq(new byte[]{9, 9}));
    }

    @Test
    void fileReplacementFailureDoesNotTouchDatabase() {
        when(repository.findById(1L, 2L)).thenReturn(Optional.of(fileDoc(2L)));
        when(repository.findSourceFileByDocument(1L, 2L)).thenReturn(Optional.of(mdSource(9L, 2L, "old", "AVAILABLE")));
        when(repository.findSourceFileBySha(eq(1L), any())).thenReturn(Optional.empty());
        when(fileStore.readManagedForReplace(1L, "knowledge/sources/1/old.md")).thenReturn(new byte[]{1});
        when(fileStore.stageReplacement(any())).thenReturn(Path.of("/tmp/s.part"));
        doThrow(new KnowledgeImportException("COPY_FAILED", "替换失败")).when(fileStore).commitReplacement(any(), any());

        assertThatThrownBy(() -> service.saveContent(2L, "# 新"))
                .isInstanceOf(KnowledgeImportException.class);
        verify(repository, never()).saveNoteContent(anyLong(), anyLong(), any());
        verify(fileStore).deleteQuietly(eq(Path.of("/tmp/s.part")));
    }

    @Test
    void enforcesOneMibBoundary() {
        when(repository.findById(1L, 1L)).thenReturn(Optional.of(note(1L)));
        assertThat(service.saveContent(1L, "a".repeat(1024 * 1024)).content()).hasSize(1024 * 1024);
        assertThatThrownBy(() -> service.saveContent(1L, "a".repeat(1024 * 1024 + 1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("1 MiB");
    }
}
