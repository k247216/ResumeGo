package com.resumego.knowledge;

import com.resumego.common.CurrentUser;
import com.resumego.knowledge.dto.KnowledgeImportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 文件导入与确定性 UTF-8 文本提取。
 * 服务端只信任文件字节：MIME 一律忽略，文件名仅用于提取安全的扩展名，绝不保存客户端路径。
 */
@Service
public class KnowledgeImportService {

    public static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    private static final String SOURCE_FILE = "FILE";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_METADATA_ONLY = "METADATA_ONLY";
    private static final String JOB_RUNNING = "RUNNING";

    private static final Logger log = LoggerFactory.getLogger(KnowledgeImportService.class);

    private final KnowledgeRepository repository;
    private final KnowledgeFileStore fileStore;

    public KnowledgeImportService(KnowledgeRepository repository, KnowledgeFileStore fileStore) {
        this.repository = repository;
        this.fileStore = fileStore;
    }

    public KnowledgeImportResponse importFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.INVALID_FILENAME, "请选择要导入的文件");
        }
        KnowledgeFileNames.ParsedFileName parsed = KnowledgeFileNames.parse(file.getOriginalFilename());
        byte[] bytes = readBytes(file);
        String sha256 = sha256(bytes);

        Optional<KnowledgeSourceFile> existing = repository.findSourceFileBySha(userId(), sha256);
        if (existing.isPresent()) {
            return duplicateResponse(existing.get());
        }

        Path staged = fileStore.stage(bytes);

        String localTitle = KnowledgeFileNames.basename(parsed);
        boolean parseable = KnowledgeFileTypes.isParseable(parsed.extension());
        KnowledgeImportIds ids;
        try {
            ids = repository.insertImportRecords(userId(), localTitle, new KnowledgeSourceFileDraft(
                    parsed.originalName(),
                    fileStore.sourceRelativePath(userId(), sha256, parsed.extension()),
                    parsed.extension(),
                    bytes.length,
                    sha256,
                    "STAGED",
                    fileStore.relativePath(staged),
                    KnowledgeFileTypes.mediaTypeOf(parsed.extension())));
        } catch (DuplicateKeyException exception) {
            // 并发同 fingerprint：唯一约束兜底，事务已回滚，不创建第二份
            fileStore.deleteQuietly(staged);
            return duplicateResponse(repository.findSourceFileBySha(userId(), sha256).orElseThrow());
        } catch (RuntimeException exception) {
            fileStore.deleteQuietly(staged);
            throw exception;
        }

        repository.updateImportJobStatus(ids.importJobId(), JOB_RUNNING, null);

        try {
            fileStore.moveToSources(userId(), sha256, parsed.extension(), staged);
            repository.updateSourceStagingPath(ids.sourceFileId(), null);
        } catch (KnowledgeImportException exception) {
            repository.failImport(ids.documentId(), ids.sourceFileId(), ids.importJobId(),
                    exception.errorCode(), false);
            log.warn("知识文件导入失败 code={}", exception.errorCode());
            return failedResponse(ids.documentId(), exception.errorCode());
        }

        if (parseable) {
            try {
                String content = "docx".equals(parsed.extension())
                        ? KnowledgeTextExtractor.extractDocx(bytes)
                        : KnowledgeTextExtractor.decodeUtf8(bytes);
                repository.completeImport(ids.documentId(), ids.sourceFileId(), ids.importJobId(),
                        userId(), content);
            } catch (KnowledgeImportException exception) {
                repository.failImport(ids.documentId(), ids.sourceFileId(), ids.importJobId(),
                        exception.errorCode(), true);
                log.warn("知识文件导入失败 code={} msg={}", exception.errorCode(), exception.getMessage());
                return failedResponse(ids.documentId(), exception.errorCode());
            }
            log.info("知识文件导入完成");
            return new KnowledgeImportResponse(ids.documentId(), SOURCE_FILE, STATUS_COMPLETED, false, null);
        }

        // 非可解析类型（pdf/doc/xlsx/pptx/图片等任意扩展名）：安全副本与元数据入库，
        // 文档 METADATA_ONLY（真实状态，不伪造正文/可编辑），job COMPLETED —— 任何合法文件都可导入
        repository.completeMetadataOnlyImport(ids.documentId(), ids.sourceFileId(), ids.importJobId());
        log.info("知识文件仅收录元数据 extension={}", parsed.extension());
        return new KnowledgeImportResponse(ids.documentId(), SOURCE_FILE, STATUS_METADATA_ONLY, false, null);
    }

    private KnowledgeImportResponse duplicateResponse(KnowledgeSourceFile sourceFile) {
        KnowledgeDocument document = repository.findById(userId(), sourceFile.documentId()).orElseThrow();
        return new KnowledgeImportResponse(document.id(), SOURCE_FILE, document.processingStatus(), true, null);
    }

    private KnowledgeImportResponse failedResponse(long documentId, String errorCode) {
        return new KnowledgeImportResponse(documentId, SOURCE_FILE, STATUS_FAILED, false, errorCode);
    }

    private byte[] readBytes(MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.READ_FAILED, "读取上传文件失败");
        }
        if (bytes.length > MAX_FILE_BYTES) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.FILE_TOO_LARGE, "文件不能超过 10 MiB");
        }
        return bytes;
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
