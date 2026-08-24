package com.resumego.knowledge;

import com.resumego.common.CurrentUser;
import com.resumego.knowledge.dto.KnowledgeManagedSourceResponse;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 内部受管原文查询：只允许 main 进程经 internal token 调用。
 * 返回当前用户、FILE + AVAILABLE 文档的受管相对路径；文件缺失时更新 availability=MISSING。
 */
@Service
public class KnowledgeInternalSourceService {

    private static final String SOURCE_FILE = "FILE";
    private static final String AVAIL_AVAILABLE = "AVAILABLE";
    private static final String AVAIL_MISSING = "MISSING";

    private final KnowledgeRepository repository;
    private final KnowledgeFileStore fileStore;

    public KnowledgeInternalSourceService(KnowledgeRepository repository, KnowledgeFileStore fileStore) {
        this.repository = repository;
        this.fileStore = fileStore;
    }

    public KnowledgeManagedSourceResponse managedSource(long documentId) {
        KnowledgeDocument doc = repository.findById(userId(), documentId)
                .orElseThrow(() -> new ManagedSourceException("SOURCE_NOT_FOUND"));
        if (!SOURCE_FILE.equals(doc.sourceType())) {
            throw new ManagedSourceException("SOURCE_NOT_FILE");
        }
        KnowledgeSourceFile source = repository.findSourceFileByDocument(userId(), documentId)
                .orElseThrow(() -> new ManagedSourceException("SOURCE_NOT_FOUND"));
        if (!AVAIL_AVAILABLE.equals(source.availability())) {
            throw new ManagedSourceException("SOURCE_NOT_AVAILABLE");
        }
        Path resolved = fileStore.resolveStored(source.storedRelativePath());
        if (!Files.exists(resolved)) {
            repository.updateSourceAvailability(source.id(), AVAIL_MISSING);
            throw new ManagedSourceException("SOURCE_MISSING");
        }
        return new KnowledgeManagedSourceResponse(source.storedRelativePath());
    }

    private long userId() {
        return CurrentUser.DEMO_USER_ID;
    }
}
