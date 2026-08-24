package com.resumego.knowledge;

import java.time.LocalDateTime;

public record KnowledgeTag(
        Long id,
        Long userId,
        String name,
        String normalizedName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
