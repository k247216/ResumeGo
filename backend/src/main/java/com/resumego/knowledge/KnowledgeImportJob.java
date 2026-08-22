package com.resumego.knowledge;

import java.time.LocalDateTime;

public record KnowledgeImportJob(
        Long id,
        Long documentId,
        Long userId,
        Long sourceFileId,
        String jobStatus,
        String errorCode,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
