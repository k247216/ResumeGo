package com.resumego.knowledge;

import com.resumego.knowledge.dto.KnowledgeDeletionImpactResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KnowledgeRecoveryServiceTest {

    @TempDir
    Path tempDir;

    private KnowledgeRepository repository;
    private KnowledgeFileStore fileStore;
    private KnowledgeRecoveryService service;

    @BeforeEach
    void setUp() {
        repository = mock(KnowledgeRepository.class);
        fileStore = new KnowledgeFileStore(tempDir);
        service = new KnowledgeRecoveryService(repository, fileStore);
    }

    private KnowledgeDocument doc(long id, String sourceType, String status) {
        return new KnowledgeDocument(id, 1L, "标题", sourceType, status, LocalDateTime.now(), LocalDateTime.now());
    }

    private KnowledgeSourceFile source(long id, long docId, String sha, String availability, String stagingPath) {
        return new KnowledgeSourceFile(id, docId, 1L, "a.md", "knowledge/sources/1/" + sha + ".md",
                null, "md", 4, sha, availability, stagingPath, LocalDateTime.now(), LocalDateTime.now());
    }

    private KnowledgeImportJob job(long id, long docId, String status, String errorCode) {
        return new KnowledgeImportJob(id, docId, 1L, 9L, status, errorCode,
                null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void retryRejectsNonFileAndNonFailedStates() {
        when(repository.findById(1L, 1L)).thenReturn(Optional.of(doc(1L, "NOTE", "FAILED")));
        assertThatThrownBy(() -> service.retry(1L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("NOT_RETRYABLE");

        when(repository.findById(1L, 2L)).thenReturn(Optional.of(doc(2L, "FILE", "COMPLETED")));
        assertThatThrownBy(() -> service.retry(2L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("NOT_RETRYABLE");
    }

    @Test
    void retryRejectsNonRetryableErrorCodes() {
        when(repository.findById(1L, 1L)).thenReturn(Optional.of(doc(1L, "FILE", "FAILED")));
        when(repository.findSourceFileByDocument(1L, 1L)).thenReturn(Optional.of(source(1L, 1L, "s1", "AVAILABLE", null)));
        when(repository.findImportJobByDocument(1L, 1L)).thenReturn(Optional.of(job(1L, 1L, "FAILED", "INVALID_UTF8")));
        assertThatThrownBy(() -> service.retry(1L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("INVALID_UTF8");
        verify(repository, never()).claimImportJobForRetry(anyLong(), anyLong());
    }

    @Test
    void retryRejectsMissingStagingCopyForCopyFailed() {
        when(repository.findById(1L, 1L)).thenReturn(Optional.of(doc(1L, "FILE", "FAILED")));
        when(repository.findSourceFileByDocument(1L, 1L)).thenReturn(Optional.of(source(1L, 1L, "s1", "STAGED", null)));
        when(repository.findImportJobByDocument(1L, 1L)).thenReturn(Optional.of(job(1L, 1L, "FAILED", "COPY_FAILED")));
        assertThatThrownBy(() -> service.retry(1L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("STAGING_MISSING");
    }

    @Test
    void concurrentRetryClaimReturnsConflict() {
        when(repository.findById(1L, 1L)).thenReturn(Optional.of(doc(1L, "FILE", "FAILED")));
        when(repository.findSourceFileByDocument(1L, 1L)).thenReturn(Optional.of(source(1L, 1L, "s1", "AVAILABLE", null)));
        when(repository.findImportJobByDocument(1L, 1L)).thenReturn(Optional.of(job(1L, 1L, "FAILED", "EXTRACTION_FAILED")));
        when(repository.claimImportJobForRetry(1L, 1L)).thenReturn(false);
        assertThatThrownBy(() -> service.retry(1L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("ALREADY_RUNNING");
    }

    @Test
    void deletionImpactReturnsFlagsAndStoresTokenHash() {
        when(repository.findById(1L, 5L)).thenReturn(Optional.of(doc(5L, "FILE", "COMPLETED")));
        when(repository.findSourceFileByDocument(1L, 5L)).thenReturn(Optional.of(source(1L, 5L, "s5", "AVAILABLE", null)));
        when(repository.findExtractedContentByDocument(1L, 5L)).thenReturn(Optional.of(
                new KnowledgeExtractedContent(1L, 5L, 1L, "正文", LocalDateTime.now(), LocalDateTime.now())));
        when(repository.findDocumentCategory(1L, 5L)).thenReturn(Optional.of(
                new KnowledgeCategory(1L, 1L, "求职", "求职", null, LocalDateTime.now(), LocalDateTime.now())));
        when(repository.listDocumentTags(1L, 5L)).thenReturn(List.of(
                new KnowledgeTag(1L, 1L, "标签", "标签", LocalDateTime.now(), LocalDateTime.now())));

        KnowledgeDeletionImpactResponse impact = service.deletionImpact(5L);

        assertThat(impact.title()).isEqualTo("标题");
        assertThat(impact.hasSource()).isTrue();
        assertThat(impact.hasContent()).isTrue();
        assertThat(impact.hasCategory()).isTrue();
        assertThat(impact.hasTags()).isTrue();
        assertThat(impact.confirmationToken()).hasSize(32);
        assertThat(impact.expiresAt()).isAfter(LocalDateTime.now());
        // 明文 token 不入库：入库的是 SHA-256
        var captor = ArgumentCaptor.forClass(String.class);
        verify(repository).replaceDeletionConfirmation(eq(1L), eq(5L), captor.capture(), any());
        assertThat(captor.getValue()).isNotEqualTo(impact.confirmationToken());
        assertThat(captor.getValue()).isEqualTo(sha256Hex(impact.confirmationToken()));
    }

    @Test
    void deleteDocumentRejectsMissingExpiredConsumedAndInvalidTokens() {
        when(repository.findById(1L, 1L)).thenReturn(Optional.of(doc(1L, "NOTE", "COMPLETED")));
        assertThatThrownBy(() -> service.deleteDocument(1L, "   "))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("TOKEN_REQUIRED");

        when(repository.findDeletionConfirmation(1L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteDocument(1L, "abc"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("TOKEN_REQUIRED");

        KnowledgeDeleteConfirmation expired = new KnowledgeDeleteConfirmation(1L, 1L, 1L, sha256Hex("tok"),
                LocalDateTime.now().minusMinutes(1), null, LocalDateTime.now());
        when(repository.findDeletionConfirmation(1L, 1L)).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.deleteDocument(1L, "tok"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("TOKEN_EXPIRED");

        KnowledgeDeleteConfirmation consumed = new KnowledgeDeleteConfirmation(1L, 1L, 1L, sha256Hex("tok"),
                LocalDateTime.now().plusMinutes(10), LocalDateTime.now(), LocalDateTime.now());
        when(repository.findDeletionConfirmation(1L, 1L)).thenReturn(Optional.of(consumed));
        assertThatThrownBy(() -> service.deleteDocument(1L, "tok"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("TOKEN_CONSUMED");

        KnowledgeDeleteConfirmation valid = new KnowledgeDeleteConfirmation(1L, 1L, 1L, sha256Hex("real-token"),
                LocalDateTime.now().plusMinutes(10), null, LocalDateTime.now());
        when(repository.findDeletionConfirmation(1L, 1L)).thenReturn(Optional.of(valid));
        assertThatThrownBy(() -> service.deleteDocument(1L, "wrong-token"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("TOKEN_INVALID");
        verify(repository, never()).deleteDocumentById(anyLong(), anyLong());
    }

    @Test
    void deleteManagedRejectsTraversalAndForeignUserPaths() throws Exception {
        Files.createDirectories(tempDir.resolve("knowledge/sources/1"));
        Files.createDirectories(tempDir.resolve("knowledge/sources/2"));
        Files.write(tempDir.resolve("knowledge/sources/1/ok.md"), new byte[]{1});
        Files.write(tempDir.resolve("knowledge/sources/2/other.md"), new byte[]{1});

        // 越界路径拒绝
        assertThatThrownBy(() -> fileStore.deleteManaged(1L, "knowledge/../secret"))
                .isInstanceOf(KnowledgeImportException.class);
        assertThatThrownBy(() -> fileStore.deleteManaged(1L, "../outside"))
                .isInstanceOf(KnowledgeImportException.class);
        // 其他用户目录拒绝
        assertThatThrownBy(() -> fileStore.deleteManaged(1L, "knowledge/sources/2/other.md"))
                .isInstanceOf(KnowledgeImportException.class);
        assertThat(tempDir.resolve("knowledge/sources/2/other.md")).exists();

        // 合法路径删除
        fileStore.deleteManaged(1L, "knowledge/sources/1/ok.md");
        assertThat(tempDir.resolve("knowledge/sources/1/ok.md")).doesNotExist();
    }

    @Test
    void cleanupJobRetryRejectsWrongStatusAndMissingJob() {
        when(repository.findCleanupJobById(1L, 404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.retryCleanupJob(404L))
                .isInstanceOf(java.util.NoSuchElementException.class);

        KnowledgeCleanupJob done = new KnowledgeCleanupJob(1L, 1L, 5L, null,
                "COMPLETED", null, LocalDateTime.now(), null, null);
        when(repository.findCleanupJobById(1L, 1L)).thenReturn(Optional.of(done));
        assertThatThrownBy(() -> service.retryCleanupJob(1L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("NOT_RETRYABLE");
    }

    private String sha256Hex(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
