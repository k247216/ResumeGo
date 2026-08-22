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
        // cleanup job COMPLETED 且不再保留受管路径（彻底清理）
        List<KnowledgeCleanupJob> jobs = repository.listCleanupJobsByStatus(1L, "COMPLETED");
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).sourceRelativePath()).isNull();
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
        long pendingJob = repository.insertCleanupJob(1L, 999L, "knowledge/sources/1/sha-pending.md", "PENDING");
        // PENDING job + 越界路径 → FAILED（路径约束拒绝）
        long badJob = repository.insertCleanupJob(1L, 998L, "knowledge/../secret.md", "PENDING");

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
        long jobId = repository.insertCleanupJob(1L, 997L, "knowledge/sources/1/sha-failed.md", "PENDING");
        repository.claimCleanupJob(1L, jobId, "PENDING", "RUNNING");
        repository.updateCleanupJobStatus(jobId, "FAILED", "FILE_DELETE_FAILED");

        recovery.retryCleanupJob(jobId);

        assertThat(target).doesNotExist();
        assertThat(repository.findCleanupJobById(1L, jobId).orElseThrow().jobStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deletingCopyFailedDocumentRemovesStagingCopy() throws Exception {
        // COPY_FAILED：source STAGED + staging 路径记录，staging 文件真实存在
        KnowledgeImportIds ids = repository.insertImportRecords(1L, "复制失败", new KnowledgeSourceFileDraft(
                "副本.md", "knowledge/sources/1/sha-cp.md", "md", 10, "sha-cp", "STAGED",
                "knowledge/staging/cp.part"));
        Files.createDirectories(StoreConfig.dataDir.resolve("knowledge/staging"));
        Files.write(StoreConfig.dataDir.resolve("knowledge/staging/cp.part"), "副本".getBytes(StandardCharsets.UTF_8));
        repository.failImport(ids.documentId(), ids.sourceFileId(), ids.importJobId(), "COPY_FAILED", false);

        KnowledgeDeletionImpactResponse impact = recovery.deletionImpact(ids.documentId());
        recovery.deleteDocument(ids.documentId(), impact.confirmationToken());

        // staging 副本消失，不是 null job 伪造完成（stored + staging 各一个真实 job）
        assertThat(StoreConfig.dataDir.resolve("knowledge/staging/cp.part")).doesNotExist();
        List<KnowledgeCleanupJob> completed = repository.listCleanupJobsByStatus(1L, "COMPLETED");
        assertThat(completed).hasSize(2);
        assertThat(completed).allMatch(j -> j.sourceRelativePath() == null);
    }

    @Test
    void stuckRunningImportJobsAreResetToFailedOnStartupRecovery() throws Exception {
        // 构造：document FAILED + job RUNNING（进程崩溃残留，与真实 retry 状态一致）
        KnowledgeImportIds ids = repository.insertImportRecords(1L, "卡住", new KnowledgeSourceFileDraft(
                "卡.md", "knowledge/sources/1/sha-stuck.md", "md", 10, "sha-stuck", "STAGED", null));
        Path target = StoreConfig.dataDir.resolve("knowledge/sources/1/sha-stuck.md");
        Files.createDirectories(target.getParent());
        Files.write(target, "# 卡住正文".getBytes(StandardCharsets.UTF_8));
        repository.failImport(ids.documentId(), ids.sourceFileId(), ids.importJobId(), "EXTRACTION_FAILED", true);
        repository.claimImportJobForRetry(1L, ids.documentId()); // job RUNNING，document 仍 FAILED

        recovery.recoverStuckImportJobs();

        KnowledgeImportJob job = repository.findImportJobByDocument(1L, ids.documentId()).orElseThrow();
        assertThat(job.jobStatus()).isEqualTo("FAILED");
        assertThat(job.errorCode()).isEqualTo("EXTRACTION_FAILED");
        // 恢复后可再次重试成功
        var response = recovery.retry(ids.documentId());
        assertThat(response.processingStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void retriesCopyFailedWithStoredTargetAlreadyPlaced() throws Exception {
        // COPY_FAILED 但 staging 已消失、stored target 已存在（移动后崩溃窗口）→ 按已落位继续提取
        KnowledgeImportIds ids = repository.insertImportRecords(1L, "移动窗口", new KnowledgeSourceFileDraft(
                "窗口.md", "knowledge/sources/1/sha-window.md", "md", 10, "sha-window", "STAGED",
                "knowledge/staging/window.part"));
        Path target = StoreConfig.dataDir.resolve("knowledge/sources/1/sha-window.md");
        Files.createDirectories(target.getParent());
        Files.write(target, "# 已落位正文".getBytes(StandardCharsets.UTF_8));
        repository.failImport(ids.documentId(), ids.sourceFileId(), ids.importJobId(), "COPY_FAILED", false);

        var response = recovery.retry(ids.documentId());

        assertThat(response.processingStatus()).isEqualTo("COMPLETED");
        assertThat(repository.findExtractedContentByDocument(1L, ids.documentId()).orElseThrow().content())
                .isEqualTo("# 已落位正文");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deletingCopyFailedCrashWindowRemovesStoredTarget() throws Exception {
        // 移动后崩溃窗口：availability=STAGED、staging 已消失、stored target 已存在
        KnowledgeImportIds ids = repository.insertImportRecords(1L, "崩溃窗口", new KnowledgeSourceFileDraft(
                "窗口.md", "knowledge/sources/1/sha-crash.md", "md", 10, "sha-crash", "STAGED",
                "knowledge/staging/crash.part"));
        Path target = StoreConfig.dataDir.resolve("knowledge/sources/1/sha-crash.md");
        Files.createDirectories(target.getParent());
        Files.write(target, "# 已落位".getBytes(StandardCharsets.UTF_8));
        repository.failImport(ids.documentId(), ids.sourceFileId(), ids.importJobId(), "COPY_FAILED", false);

        KnowledgeDeletionImpactResponse impact = recovery.deletionImpact(ids.documentId());
        recovery.deleteDocument(ids.documentId(), impact.confirmationToken());

        // stored 文件不再遗留（即使 source availability=STAGED）；stored + staging 各一个真实 job
        assertThat(target).doesNotExist();
        List<KnowledgeCleanupJob> completed = repository.listCleanupJobsByStatus(1L, "COMPLETED");
        assertThat(completed).hasSize(2);
        assertThat(completed).allMatch(j -> j.sourceRelativePath() == null);
    }

    @Test
    void retryRemovesExtraStagingWhenStoredTargetAlsoExists() throws Exception {
        // staging 与 stored 并存：成功后多余 staging 被删除且路径清空
        KnowledgeImportIds ids = repository.insertImportRecords(1L, "并存", new KnowledgeSourceFileDraft(
                "并存.md", "knowledge/sources/1/sha-both.md", "md", 10, "sha-both", "STAGED",
                "knowledge/staging/both.part"));
        Path target = StoreConfig.dataDir.resolve("knowledge/sources/1/sha-both.md");
        Files.createDirectories(target.getParent());
        Files.write(target, "# 并存正文".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(StoreConfig.dataDir.resolve("knowledge/staging"));
        Files.write(StoreConfig.dataDir.resolve("knowledge/staging/both.part"), "残留".getBytes(StandardCharsets.UTF_8));
        repository.failImport(ids.documentId(), ids.sourceFileId(), ids.importJobId(), "COPY_FAILED", false);

        var response = recovery.retry(ids.documentId());

        assertThat(response.processingStatus()).isEqualTo("COMPLETED");
        assertThat(StoreConfig.dataDir.resolve("knowledge/staging/both.part")).doesNotExist();
        assertThat(repository.findSourceFileByDocument(1L, ids.documentId()).orElseThrow().stagingRelativePath()).isNull();
    }
}
