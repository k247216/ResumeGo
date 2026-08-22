package com.resumego.knowledge;

import com.resumego.knowledge.dto.KnowledgeDeletionImpactResponse;
import com.resumego.knowledge.dto.KnowledgeSearchItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@Commit
@Import({KnowledgeRepository.class, KnowledgeRecoveryService.class,
        KnowledgeClassificationService.class,
        KnowledgeRecoveryIntegrationTest.StoreConfig.class})
@Sql(scripts = "/sql/knowledge_schema.sql")
class KnowledgeRecoveryIntegrationTest {

    @Autowired
    KnowledgeRepository repository;

    @Autowired
    KnowledgeRecoveryService recovery;

    @Autowired
    KnowledgeClassificationService classification;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @TestConfiguration
    static class StoreConfig {
        static Path dataDir;

        @Bean
        KnowledgeFileStore knowledgeFileStore() {
            dataDir = Path.of(System.getProperty("java.io.tmpdir"), "be03-it-" + UUID.randomUUID());
            return new KnowledgeFileStore(dataDir);
        }
    }

    private KnowledgeFileStore store() {
        return new KnowledgeFileStore(StoreConfig.dataDir);
    }

    @BeforeEach
    void cleanDataDir() throws Exception {
        if (StoreConfig.dataDir != null && Files.exists(StoreConfig.dataDir)) {
            try (var walk = Files.walk(StoreConfig.dataDir)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception ignored) { }
                });
            }
        }
    }

    private long failedFileDocument(String sha, String errorCode, String content) throws Exception {
        KnowledgeImportIds ids = repository.insertImportRecords(1L, "失败文件", new KnowledgeSourceFileDraft(
                "失败.md", "knowledge/sources/1/" + sha + ".md", "md", 10, sha, "STAGED", null));
        String relative = "knowledge/sources/1/" + sha + ".md";
        Path target = StoreConfig.dataDir.resolve(relative);
        Files.createDirectories(target.getParent());
        Files.write(target, content.getBytes(StandardCharsets.UTF_8));
        repository.failImport(ids.documentId(), ids.sourceFileId(), ids.importJobId(), errorCode, true);
        return ids.documentId();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deletesDocumentAndFilesAfterCommitWithCleanupCompleted() throws Exception {
        // 完整文档：content + category + tag
        long docId = failedFileDocument("sha-del", "EXTRACTION_FAILED", "# 待删除");
        long categoryId = repository.insertCategory(1L, "求职", "求职");
        long tagId = repository.insertTag(1L, "标签", "标签");
        repository.setDocumentCategory(1L, docId, categoryId);
        repository.addDocumentTag(1L, docId, tagId);

        KnowledgeDeletionImpactResponse impact = recovery.deletionImpact(docId);
        assertThat(impact.hasSource()).isTrue();

        recovery.deleteDocument(docId, impact.confirmationToken());

        // 文档/关联/正文/分类关联/标签关联全部消失（FK cascade）
        assertThat(repository.findById(1L, docId)).isEmpty();
        assertThat(repository.findExtractedContentByDocument(1L, docId)).isEmpty();
        assertThat(repository.findSourceFileByDocument(1L, docId)).isEmpty();
        assertThat(repository.findDocumentCategory(1L, docId)).isEmpty();
        assertThat(repository.listDocumentTags(1L, docId)).isEmpty();
        // 受管文件已删除
        assertThat(StoreConfig.dataDir.resolve("knowledge/sources/1/sha-del.md")).doesNotExist();
        // cleanup job COMPLETED
        List<KnowledgeCleanupJob> jobs = repository.listCleanupJobsByStatus(1L, "COMPLETED");
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).sourceRelativePath()).isEqualTo("knowledge/sources/1/sha-del.md");
    }

    @Test
    void deletedDocumentIsInvisibleToSearchAndOthersUnaffected() throws Exception {
        long keepDocId = failedFileDocument("sha-keep", "EXTRACTION_FAILED", "保留文档内容");
        long deleteDocId = failedFileDocument("sha-gone", "EXTRACTION_FAILED", "要删除的内容");
        KnowledgeDeletionImpactResponse impact = recovery.deletionImpact(deleteDocId);
        recovery.deleteDocument(deleteDocId, impact.confirmationToken());

        List<KnowledgeSearchItemResponse> results = classification.search("失败文件", null, null);
        assertThat(results).noneMatch(r -> r.document().id() == deleteDocId);
        assertThat(results).anyMatch(r -> r.document().id() == keepDocId);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void databaseRollbackDoesNotDeleteFilesOnInvalidToken() throws Exception {
        long docId = failedFileDocument("sha-rollback", "EXTRACTION_FAILED", "回滚测试");
        KnowledgeDeletionImpactResponse impact = recovery.deletionImpact(docId);

        assertThatThrownBy(() -> recovery.deleteDocument(docId, "wrong-token"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("TOKEN_INVALID");

        // 事务回滚：文档仍在、文件未删、confirmation 未消耗
        assertThat(repository.findById(1L, docId)).isPresent();
        assertThat(StoreConfig.dataDir.resolve("knowledge/sources/1/sha-rollback.md")).exists();
        assertThat(repository.findDeletionConfirmation(1L, docId).orElseThrow().consumedAt()).isNull();
    }

    @Test
    void retriesExtractionFailedFromManagedSource() throws Exception {
        long docId = failedFileDocument("sha-retry", "EXTRACTION_FAILED", "# 重试正文");
        var response = recovery.retry(docId);
        assertThat(response.processingStatus()).isEqualTo("COMPLETED");
        assertThat(repository.findExtractedContentByDocument(1L, docId).orElseThrow().content())
                .isEqualTo("# 重试正文");
    }

    @Test
    void retriesCopyFailedFromRecordedStagingPath() throws Exception {
        // COPY_FAILED：staging 文件仍在，source STAGED
        KnowledgeImportIds ids = repository.insertImportRecords(1L, "复制失败", new KnowledgeSourceFileDraft(
                "副本.md", "knowledge/sources/1/sha-copy.md", "md", 10, "sha-copy", "STAGED",
                "knowledge/staging/copy.part"));
        Files.createDirectories(StoreConfig.dataDir.resolve("knowledge/staging"));
        Files.write(StoreConfig.dataDir.resolve("knowledge/staging/copy.part"), "副本正文".getBytes(StandardCharsets.UTF_8));
        repository.failImport(ids.documentId(), ids.sourceFileId(), ids.importJobId(), "COPY_FAILED", false);

        var response = recovery.retry(ids.documentId());

        assertThat(response.processingStatus()).isEqualTo("COMPLETED");
        assertThat(StoreConfig.dataDir.resolve("knowledge/sources/1/sha-copy.md")).exists();
        assertThat(StoreConfig.dataDir.resolve("knowledge/staging/copy.part")).doesNotExist();
        assertThat(repository.findSourceFileByDocument(1L, ids.documentId()).orElseThrow().stagingRelativePath()).isNull();
    }

    @Test
    void pendingCleanupJobsAreRecoveredOnStartupAndFailuresBecomeFailed() throws Exception {
        // PENDING job + 文件存在 → 恢复成功
        Path target = StoreConfig.dataDir.resolve("knowledge/sources/1/sha-pending.md");
        Files.createDirectories(target.getParent());
        Files.write(target, "x".getBytes(StandardCharsets.UTF_8));
        long pendingJob = repository.insertCleanupJob(1L, 999L, "遗留文档", "knowledge/sources/1/sha-pending.md", "PENDING");
        // PENDING job + 越界路径 → FAILED（路径约束拒绝）
        long badJob = repository.insertCleanupJob(1L, 998L, "坏路径", "knowledge/../secret.md", "PENDING");

        recovery.recoverPendingCleanupJobs();

        assertThat(target).doesNotExist();
        assertThat(repository.findCleanupJobById(1L, pendingJob).orElseThrow().jobStatus()).isEqualTo("COMPLETED");
        KnowledgeCleanupJob failed = repository.findCleanupJobById(1L, badJob).orElseThrow();
        assertThat(failed.jobStatus()).isEqualTo("FAILED");
        assertThat(failed.errorCode()).isEqualTo("FILE_DELETE_FAILED");
    }

    @Test
    void retryCleanupJobResetsFailedToPendingAndExecutes() throws Exception {
        Path target = StoreConfig.dataDir.resolve("knowledge/sources/1/sha-failed.md");
        Files.createDirectories(target.getParent());
        Files.write(target, "x".getBytes(StandardCharsets.UTF_8));
        long jobId = repository.insertCleanupJob(1L, 997L, "清理", "knowledge/sources/1/sha-failed.md", "PENDING");
        repository.claimCleanupJob(1L, jobId, "PENDING", "RUNNING");
        repository.updateCleanupJobStatus(jobId, "FAILED", "FILE_DELETE_FAILED");

        recovery.retryCleanupJob(jobId);

        assertThat(target).doesNotExist();
        assertThat(repository.findCleanupJobById(1L, jobId).orElseThrow().jobStatus()).isEqualTo("COMPLETED");
    }
}
