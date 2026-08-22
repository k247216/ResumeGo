package com.resumego.knowledge;

public record KnowledgeSourceFileDraft(
        String originalName,
        String storedRelativePath,
        String extension,
        long sizeBytes,
        String sha256,
        String availability,
        String stagingRelativePath
) {
    /** 兼容无 staging 路径的旧调用（staging 相对路径默认为 null）。 */
    public KnowledgeSourceFileDraft(
            String originalName,
            String storedRelativePath,
            String extension,
            long sizeBytes,
            String sha256,
            String availability) {
        this(originalName, storedRelativePath, extension, sizeBytes, sha256, availability, null);
    }
}
