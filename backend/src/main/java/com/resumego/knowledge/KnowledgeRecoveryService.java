package com.resumego.knowledge;

import com.resumego.common.CurrentUser;
import com.resumego.knowledge.dto.KnowledgeDeletionImpactResponse;
import com.resumego.knowledge.dto.KnowledgeDocumentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 解析重试与可恢复删除。
 * 所有权贯穿所有操作；明文 token 不入库不入日志；文件删除在事务提交后进行。
 */
@Service
public class KnowledgeRecoveryService {

    private static final String SOURCE_FILE = "FILE";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String AVAIL_AVAILABLE = "AVAILABLE";
    private static final String CODE_COPY_FAILED = "COPY_FAILED";
    private static final String CODE_EXTRACTION_FAILED = "EXTRACTION_FAILED";
    private static final String CODE_INVALID_UTF8 = "INVALID_UTF8";
    private static final long CONFIRMATION_TTL_MINUTES = 10;
    private static final String FILE_DELETE_FAILED = "FILE_DELETE_FAILED";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final KnowledgeRepository repository;
    private final KnowledgeFileStore fileStore;

    public KnowledgeRecoveryService(KnowledgeRepository repository, KnowledgeFileStore fileStore) {
        this.repository = repository;
        this.fileStore = fileStore;
    }

    // ---- retry ----

    /**
     * 仅当前用户 FILE + FAILED 可重试：COPY_FAILED（staging 副本仍在）与 EXTRACTION_FAILED（source AVAILABLE）。
     * 条件更新 FAILED -> RUNNING 防并发；不接受上传字节或任意路径，不修改原始用户文件。
     */
    @Transactional
    public KnowledgeDocumentResponse retry(long documentId) {
        KnowledgeDocument doc = repository.findById(userId(), documentId)
                .orElseThrow(() -> new NoSuchElementException("知识文档不存在"));
        if (!SOURCE_FILE.equals(doc.sourceType())) {
            throw conflict("NOT_RETRYABLE", "仅文件文档支持重试");
        }
        if (!STATUS_FAILED.equals(doc.processingStatus())) {
            throw conflict("NOT_RETRYABLE", "仅 FAILED 状态的文档可重试");
        }
        KnowledgeSourceFile source = repository.findSourceFileByDocument(userId(), documentId)
                .orElseThrow(() -> new NoSuchElementException("知识来源文件不存在"));
        KnowledgeImportJob job = repository.findImportJobByDocument(userId(), documentId)
                .orElseThrow(() -> new NoSuchElementException("导入任务不存在"));
        String errorCode = job.errorCode();
        boolean copyNeedsMove = false;
        if (CODE_COPY_FAILED.equals(errorCode)) {
            String stagingPath = source.stagingRelativePath();
            boolean stagingExists = stagingPath != null && Files.exists(fileStore.resolveStored(stagingPath));
            boolean storedTargetExists = Files.exists(fileStore.resolveStored(source.storedRelativePath()));
            if (!stagingExists && !storedTargetExists) {
                throw conflict("STAGING_MISSING", "staging 副本缺失，无法重试");
            }
            // 移动后崩溃窗口：staging 已消失但确定性的 stored target 已存在 → 按文件已落位继续提取
            copyNeedsMove = stagingExists && !storedTargetExists;
        } else if (CODE_EXTRACTION_FAILED.equals(errorCode)) {
            if (!AVAIL_AVAILABLE.equals(source.availability())) {
                throw conflict("SOURCE_UNAVAILABLE", "受管副本不可用，无法重试");
            }
        } else {
            throw conflict("NOT_RETRYABLE", "该失败类型不支持重试（如 " + CODE_INVALID_UTF8 + "）");
        }
        if (!repository.claimImportJobForRetry(userId(), documentId)) {
            throw conflict("ALREADY_RUNNING", "已有重试进行中");
        }
        try {
            if (copyNeedsMove) {
                Path staged = fileStore.resolveStored(source.stagingRelativePath());
                fileStore.moveToSources(userId(), source.sha256(), source.extension(), staged);
                repository.updateSourceStagingPath(source.id(), null);
            } else {
                // staging 与 stored 并存的窗口：读取 stored 前删除多余 staging，避免泄漏
                String stagingPath = source.stagingRelativePath();
                if (stagingPath != null && Files.exists(fileStore.resolveStored(stagingPath))) {
                    fileStore.deleteManaged(userId(), stagingPath);
                    repository.updateSourceStagingPath(source.id(), null);
                }
            }
            Path sourcePath = fileStore.resolveStored(source.storedRelativePath());
            String content = KnowledgeTextExtractor.decodeUtf8(Files.readAllBytes(sourcePath));
            repository.completeImportRetry(documentId, source.id(), job.id(), userId(), content);
        } catch (IOException | KnowledgeImportException exception) {
            String failedCode = exception instanceof KnowledgeImportException
                    ? ((KnowledgeImportException) exception).errorCode() : CODE_EXTRACTION_FAILED;
            repository.failImport(documentId, source.id(), job.id(), failedCode, true);
            return toResponse(repository.findById(userId(), documentId).orElseThrow());
        }
        return toResponse(repository.findById(userId(), documentId).orElseThrow());
    }

    // ---- deletion ----

    /** 删除影响摘要 + 生成一次性确认 token（SHA-256 入库，明文只返回一次）。 */
    @Transactional
    public KnowledgeDeletionImpactResponse deletionImpact(long documentId) {
        KnowledgeDocument doc = repository.findById(userId(), documentId)
                .orElseThrow(() -> new NoSuchElementException("知识文档不存在"));
        KnowledgeSourceFile source = repository.findSourceFileByDocument(userId(), documentId).orElse(null);
        boolean hasSource = source != null && SOURCE_FILE.equals(doc.sourceType());
        boolean hasContent = repository.findExtractedContentByDocument(userId(), documentId).isPresent();
        boolean hasCategory = repository.findDocumentCategory(userId(), documentId).isPresent();
        boolean hasTags = !repository.listDocumentTags(userId(), documentId).isEmpty();
        String token = randomToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CONFIRMATION_TTL_MINUTES);
        repository.replaceDeletionConfirmation(userId(), documentId, sha256Hex(token), expiresAt);
        return new KnowledgeDeletionImpactResponse(doc.title(), hasSource, hasContent,
                hasCategory, hasTags, token, expiresAt);
    }

    /**
     * 同一事务校验当前用户、token 未失效未使用；创建 cleanup job（FILE 时）并删除 document；
     * FK cascade 清 content/import job/category/tag relation/source metadata。
     * 受管副本在事务提交后删除；失败 job=FAILED 但文档保持已删除。
     */
    @Transactional
    public void deleteDocument(long documentId, String confirmationToken) {
        KnowledgeDocument doc = repository.findById(userId(), documentId)
                .orElseThrow(() -> new NoSuchElementException("知识文档不存在"));
        if (confirmationToken == null || confirmationToken.isBlank()) {
            throw conflict("TOKEN_REQUIRED", "缺少确认令牌");
        }
        KnowledgeDeleteConfirmation confirmation = repository.findDeletionConfirmation(userId(), documentId)
                .orElseThrow(() -> conflict("TOKEN_REQUIRED", "请先获取删除确认令牌"));
        if (confirmation.consumedAt() != null) {
            throw conflict("TOKEN_CONSUMED", "确认令牌已使用");
        }
        if (confirmation.expiresAt().isBefore(LocalDateTime.now())) {
            throw conflict("TOKEN_EXPIRED", "确认令牌已过期");
        }
        byte[] expected = hexToBytes(confirmation.tokenHash());
        if (!MessageDigest.isEqual(sha256Bytes(confirmationToken), expected)) {
            throw conflict("TOKEN_INVALID", "确认令牌无效");
        }
        KnowledgeSourceFile source = repository.findSourceFileByDocument(userId(), documentId).orElse(null);
        // 为每个真实受管路径创建 cleanup job：AVAILABLE 的 stored path 与非空 staging 路径分别清理，
        // 不能用 null 路径伪造完成（COPY_FAILED 的 staging 副本也必须删除）。
        // stored 路径非空始终纳入（即使 availability=STAGED 的“移动后崩溃窗口”），
        // staging 路径非空也纳入；deleteManaged 幂等处理不存在文件；去重避免重复任务。
        List<String> managedPaths = new java.util.ArrayList<>();
        if (source != null) {
            if (source.storedRelativePath() != null) {
                managedPaths.add(source.storedRelativePath());
            }
            if (source.stagingRelativePath() != null && !managedPaths.contains(source.stagingRelativePath())) {
                managedPaths.add(source.stagingRelativePath());
            }
        }
        List<Long> cleanupJobIds = new java.util.ArrayList<>();
        if (SOURCE_FILE.equals(doc.sourceType())) {
            if (managedPaths.isEmpty()) {
                cleanupJobIds.add(repository.insertCleanupJob(userId(), documentId, null, STATUS_PENDING));
            } else {
                for (String path : managedPaths) {
                    cleanupJobIds.add(repository.insertCleanupJob(userId(), documentId, path, STATUS_PENDING));
                }
            }
        }
        repository.consumeDeletionConfirmation(confirmation.id());
        repository.deleteDocumentById(userId(), documentId);

        final List<Long> jobIds = List.copyOf(cleanupJobIds);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (long jobId : jobIds) {
                    KnowledgeCleanupJob job = repository.findCleanupJobById(userId(), jobId).orElse(null);
                    if (job == null) {
                        continue;
                    }
                    try {
                        if (job.sourceRelativePath() != null) {
                            fileStore.deleteManaged(userId(), job.sourceRelativePath());
                        }
                        repository.completeCleanupJob(jobId);
                    } catch (Exception exception) {
                        repository.updateCleanupJobStatus(jobId, STATUS_FAILED, FILE_DELETE_FAILED);
                    }
                }
            }
        });
    }

    // ---- cleanup jobs ----

    /** 手动重试清理任务：FAILED 先置回 PENDING 再处理一次；PENDING 直接 claim 执行。 */
    @Transactional
    public void retryCleanupJob(long cleanupJobId) {
        KnowledgeCleanupJob job = repository.findCleanupJobById(userId(), cleanupJobId)
                .orElseThrow(() -> new NoSuchElementException("清理任务不存在"));
        if (!STATUS_FAILED.equals(job.jobStatus()) && !STATUS_PENDING.equals(job.jobStatus())) {
            throw conflict("NOT_RETRYABLE", "仅 FAILED/PENDING 清理任务可重试");
        }
        if (STATUS_FAILED.equals(job.jobStatus())) {
            repository.claimCleanupJob(userId(), cleanupJobId, STATUS_FAILED, STATUS_PENDING);
        }
        if (!repository.claimCleanupJob(userId(), cleanupJobId, STATUS_PENDING, STATUS_RUNNING)) {
            throw conflict("ALREADY_RUNNING", "清理任务已在处理");
        }
        executeCleanup(job, cleanupJobId);
    }

    /** 启动恢复 import job：进程崩溃残留的 RUNNING job（document 仍 FAILED）重置回 FAILED，保留原 errorCode。 */
    public void recoverStuckImportJobs() {
        List<KnowledgeImportJob> stuck = repository.listStuckRunningImportJobs(userId());
        for (KnowledgeImportJob job : stuck) {
            repository.resetImportJobToFailed(job.id());
        }
    }

    /** 启动恢复：只处理 PENDING，避免无限重试 FAILED。 */
    public void recoverPendingCleanupJobs() {
        List<KnowledgeCleanupJob> pending = repository.listCleanupJobsByStatus(userId(), STATUS_PENDING);
        for (KnowledgeCleanupJob job : pending) {
            if (!repository.claimCleanupJob(userId(), job.id(), STATUS_PENDING, STATUS_RUNNING)) {
                continue;
            }
            executeCleanup(job, job.id());
        }
    }

    private void executeCleanup(KnowledgeCleanupJob job, long cleanupJobId) {
        try {
            if (job.sourceRelativePath() != null) {
                fileStore.deleteManaged(userId(), job.sourceRelativePath());
            }
            repository.completeCleanupJob(cleanupJobId);
        } catch (Exception exception) {
            repository.updateCleanupJobStatus(cleanupJobId, STATUS_FAILED, FILE_DELETE_FAILED);
        }
    }

    // ---- helpers ----

    private IllegalStateException conflict(String code, String message) {
        return new IllegalStateException(code + ": " + message);
    }

    private String randomToken() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256Hex(String input) {
        return HexFormat.of().formatHex(sha256Bytes(input));
    }

    private byte[] sha256Bytes(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private byte[] hexToBytes(String hex) {
        return HexFormat.of().parseHex(hex);
    }

    private KnowledgeDocumentResponse toResponse(KnowledgeDocument doc) {
        String sourceFile = "NOTE".equals(doc.sourceType()) ? null
                : repository.findSourceFileByDocument(userId(), doc.id())
                .map(KnowledgeSourceFile::originalName)
                .orElse(null);
        return new KnowledgeDocumentResponse(doc.id(), doc.title(), doc.sourceType(),
                doc.processingStatus(), sourceFile, doc.createdAt().toString(), doc.updatedAt().toString());
    }

    private long userId() {
        return CurrentUser.DEMO_USER_ID;
    }
}
