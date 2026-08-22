package com.resumego.knowledge;

import java.time.LocalDateTime;

public record KnowledgeCleanupJob(
        Long id,
        Long userId,
        Long documentId,
        String documentTitle,
        String sourceRelativePath,
        String jobStatus,
        String errorCode,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
