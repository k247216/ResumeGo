package com.resumego.knowledge;

import com.resumego.common.CurrentUser;
import com.resumego.knowledge.dto.KnowledgeContentResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * 受管正文保存：NOTE 直写 extracted content；Markdown 只改 ResumeGo 受管副本（可回滚），
 * 绝不写回外部原文件；TXT/未知类型/不可用源文件拒绝。
 */
@Service
public class KnowledgeManagedContentService {

    private static final long MAX_CONTENT_BYTES = 1024L * 1024;
    private static final String SOURCE_NOTE = "NOTE";
    private static final String SOURCE_FILE = "FILE";
    private static final String AVAIL_AVAILABLE = "AVAILABLE";

    private final KnowledgeRepository repository;
    private final KnowledgeFileStore fileStore;
    private final TransactionTemplate transactionTemplate;

    public KnowledgeManagedContentService(
            KnowledgeRepository repository,
            KnowledgeFileStore fileStore,
            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.fileStore = fileStore;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public KnowledgeContentResponse saveContent(long documentId, String content) {
        if (content == null) {
            throw new IllegalArgumentException("正文不能为空值");
        }
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_CONTENT_BYTES) {
            throw new IllegalArgumentException("正文不能超过 1 MiB");
        }
        KnowledgeDocument doc = repository.findById(userId(), documentId)
                .orElseThrow(() -> new NoSuchElementException("知识文档不存在"));
        if (SOURCE_NOTE.equals(doc.sourceType())) {
            return saveNoteContent(documentId, content);
        }
        return saveManagedFileContent(documentId, content, bytes);
    }

    /** NOTE：upsert 正文 + COMPLETED，同一事务。 */
    private KnowledgeContentResponse saveNoteContent(long documentId, String content) {
        transactionTemplate.executeWithoutResult(status ->
                repository.saveNoteContent(documentId, userId(), content));
        return new KnowledgeContentResponse(documentId, content);
    }

    /** Markdown 受管副本保存：文件原子替换 + DB 同步，可验证回滚。 */
    private KnowledgeContentResponse saveManagedFileContent(long documentId, String content, byte[] bytes) {
        KnowledgeSourceFile source = repository.findSourceFileByDocument(userId(), documentId)
                .orElseThrow(() -> new NoSuchElementException("知识来源文件不存在"));
        String extension = normalizeExtension(source.extension());
        if (!"md".equals(extension)) {
            throw new IllegalStateException("NOT_EDITABLE: 仅 Markdown 文件可编辑，TXT 只读");
        }
        if (!AVAIL_AVAILABLE.equals(source.availability())) {
            throw new IllegalStateException("SOURCE_NOT_AVAILABLE: 受管文件当前不可用");
        }
        String newSha = sha256(bytes);
        // 新 hash 已被同一用户另一文档占用：稳定拒绝，不覆盖不合并
        Optional<KnowledgeSourceFile> conflict = repository.findSourceFileBySha(userId(), newSha);
        if (conflict.isPresent() && conflict.get().documentId() != documentId) {
            throw new IllegalStateException("HASH_CONFLICT: 相同内容的文件已存在于资料库");
        }
        // 读取旧受管文件（校验普通文件/符号链接/越界；备份到内存用于回滚）
        fileStore.readManagedForReplace(userId(), source.storedRelativePath());
        String oldRelativePath = source.storedRelativePath();
        // 目标改为新 hash 对应的 owner 受管路径，维持 hash 命名不变量
        String newRelativePath = "knowledge/sources/" + userId() + "/" + newSha + "." + extension;
        Path newManaged = fileStore.resolveStored(newRelativePath);
        // 准备替换：staging 临时文件 → 新 hash 路径
        Path staged = fileStore.stageReplacement(bytes);
        boolean movedToNewPath = false;
        try {
            fileStore.commitReplacement(newManaged, staged);
            movedToNewPath = true;
            // DB 同步：stored_relative_path + size + sha + 提取正文；失败回滚新路径文件（旧路径文件未动）
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    repository.saveNoteContent(documentId, userId(), content);
                    repository.updateSourceFileAfterEdit(source.id(), newRelativePath, bytes.length, newSha);
                });
            } catch (RuntimeException exception) {
                fileStore.deleteQuietly(newManaged);
                throw exception;
            }
        } catch (RuntimeException exception) {
            if (!movedToNewPath) {
                fileStore.deleteQuietly(staged);
            }
            throw exception;
        }
        // 成功后释放旧 hash 路径文件（受约束删除），不遗留旧文件
        if (!oldRelativePath.equals(newRelativePath)) {
            fileStore.deleteManaged(userId(), oldRelativePath);
        }
        return new KnowledgeContentResponse(documentId, content);
    }

    /** 统一扩展名识别（小写归一，未知 → unknown）。 */
    static String normalizeExtension(String extension) {
        return KnowledgeFileTypes.normalizeExtension(extension);
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private long userId() {
        return CurrentUser.DEMO_USER_ID;
    }
}
