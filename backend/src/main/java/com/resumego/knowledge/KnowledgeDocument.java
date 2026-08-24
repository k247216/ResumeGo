package com.resumego.knowledge;

import java.time.LocalDateTime;

public record KnowledgeDocument(
        Long id,
        Long userId,
        String title,
        String sourceType,
        String processingStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
