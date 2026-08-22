package com.resumego.knowledge;

public record KnowledgeSourceFileDraft(
        String originalName,
        String storedRelativePath,
        String extension,
        long sizeBytes,
        String sha256,
        String availability
) {
}
