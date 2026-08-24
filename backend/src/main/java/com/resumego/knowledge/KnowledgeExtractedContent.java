package com.resumego.knowledge;

import java.time.LocalDateTime;

public record KnowledgeExtractedContent(
        Long id,
        Long documentId,
        Long userId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
