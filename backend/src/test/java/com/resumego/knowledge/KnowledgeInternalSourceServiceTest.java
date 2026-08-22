package com.resumego.knowledge;

import com.resumego.knowledge.dto.KnowledgeManagedSourceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KnowledgeInternalSourceServiceTest {

    @TempDir
    Path tempDir;

    private KnowledgeRepository repository;
    private KnowledgeFileStore fileStore;
    private KnowledgeInternalSourceService service;

    @BeforeEach
    void setUp() {
        repository = mock(KnowledgeRepository.class);
        fileStore = new KnowledgeFileStore(tempDir);
        service = new KnowledgeInternalSourceService(repository, fileStore);
    }

    private KnowledgeDocument doc(long id, String sourceType, String status) {
        return new KnowledgeDocument(id, 1L, "标题", sourceType, status, LocalDateTime.now(), LocalDateTime.now());
    }

    private KnowledgeSourceFile source(long id, long docId, String sha, String availability) {
        return new KnowledgeSourceFile(id, docId, 1L, "a.md",
                "knowledge/sources/1/" + sha + ".md", null, "md", 4, sha, availability, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void missingOrForeignDocumentIsSourceNotFound() {
        when(repository.findById(1L, 404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.managedSource(404L))
                .isInstanceOf(ManagedSourceException.class)
                .extracting(e -> ((ManagedSourceException) e).code())
                .isEqualTo("SOURCE_NOT_FOUND");
    }

    @Test
    void noteDocumentIsSourceNotFile() {
        when(repository.findById(1L, 1L)).thenReturn(Optional.of(doc(1L, "NOTE", "COMPLETED")));
        assertThatThrownBy(() -> service.managedSource(1L))
                .isInstanceOf(ManagedSourceException.class)
                .extracting(e -> ((ManagedSourceException) e).code())
                .isEqualTo("SOURCE_NOT_FILE");
    }

    @Test
    void stagedOrMissingAvailabilityIsSourceNotAvailable() {
        when(repository.findById(1L, 1L)).thenReturn(Optional.of(doc(1L, "FILE", "FAILED")));
        when(repository.findSourceFileByDocument(1L, 1L)).thenReturn(Optional.of(source(1L, 1L, "s1", "STAGED")));
        assertThatThrownBy(() -> service.managedSource(1L))
                .isInstanceOf(ManagedSourceException.class)
                .extracting(e -> ((ManagedSourceException) e).code())
                .isEqualTo("SOURCE_NOT_AVAILABLE");
    }

    @Test
    void missingPhysicalFileMarksSourceMissingAndKeepsDocument() throws Exception {
        when(repository.findById(1L, 1L)).thenReturn(Optional.of(doc(1L, "FILE", "COMPLETED")));
        when(repository.findSourceFileByDocument(1L, 1L)).thenReturn(Optional.of(source(1L, 1L, "s1", "AVAILABLE")));

        assertThatThrownBy(() -> service.managedSource(1L))
                .isInstanceOf(ManagedSourceException.class)
                .extracting(e -> ((ManagedSourceException) e).code())
                .isEqualTo("SOURCE_MISSING");
        // availability 更新为 MISSING，不删除文档/正文
        verify(repository).updateSourceAvailability(1L, "MISSING");
        verify(repository, never()).deleteDocumentById(anyLong(), anyLong());
    }

    @Test
    void availablePhysicalFileReturnsRelativePath() throws Exception {
        Path target = tempDir.resolve("knowledge/sources/1/s1.md");
        Files.createDirectories(target.getParent());
        Files.write(target, "正文".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(repository.findById(1L, 1L)).thenReturn(Optional.of(doc(1L, "FILE", "COMPLETED")));
        when(repository.findSourceFileByDocument(1L, 1L)).thenReturn(Optional.of(source(1L, 1L, "s1", "AVAILABLE")));

        KnowledgeManagedSourceResponse response = service.managedSource(1L);

        assertThat(response.relativePath()).isEqualTo("knowledge/sources/1/s1.md");
        verify(repository, never()).updateSourceAvailability(anyLong(), anyString());
    }
}
