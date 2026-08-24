package com.resumego.knowledge;

import java.time.LocalDateTime;

public record KnowledgeDeleteConfirmation(
        Long id,
        Long userId,
        Long documentId,
        String tokenHash,
        LocalDateTime expiresAt,
        LocalDateTime consumedAt,
        LocalDateTime createdAt
) {
}
