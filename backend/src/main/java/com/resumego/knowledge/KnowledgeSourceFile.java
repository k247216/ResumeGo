package com.resumego.knowledge;

import java.time.LocalDateTime;

public record KnowledgeSourceFile(
        Long id,
        Long documentId,
        Long userId,
        String originalName,
        String storedRelativePath,
        String mimeType,
        String extension,
        long sizeBytes,
        String sha256,
        String availability,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
