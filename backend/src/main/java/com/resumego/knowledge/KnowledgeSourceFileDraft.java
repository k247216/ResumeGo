package com.resumego.knowledge;

public record KnowledgeSourceFileDraft(
        String originalName,
        String storedRelativePath,
        String extension,
        long sizeBytes,
        String sha256,
        String availability,
        String stagingRelativePath,
        String mediaType
) {
    /** 兼容无 staging/mediaType 的旧调用（默认 null）。 */
    public KnowledgeSourceFileDraft(
            String originalName,
            String storedRelativePath,
            String extension,
            long sizeBytes,
            String sha256,
            String availability) {
        this(originalName, storedRelativePath, extension, sizeBytes, sha256, availability, null, null);
    }

    public KnowledgeSourceFileDraft(
            String originalName,
            String storedRelativePath,
            String extension,
            long sizeBytes,
            String sha256,
            String availability,
            String stagingRelativePath) {
        this(originalName, storedRelativePath, extension, sizeBytes, sha256, availability, stagingRelativePath, null);
    }
}
