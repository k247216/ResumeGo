package com.resumego.knowledge;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * V2 data dir 下的文件暂存与安全落位。
 * staging -> knowledge/staging；校验通过后原子移动到 knowledge/sources/{userId}/{sha256}.{ext}。
 * 数据库只保存相对 data dir 的相对路径。
 */
public class KnowledgeFileStore {

    private static final String STAGING_SUBDIR = "knowledge/staging";
    private static final String SOURCES_SUBDIR = "knowledge/sources";

    private final Path root;

    public KnowledgeFileStore(Path root) {
        this.root = root;
    }

    Path root() {
        return root;
    }

    public String sourceRelativePath(long userId, String sha256, String extension) {
        return SOURCES_SUBDIR + "/" + userId + "/" + sha256 + "." + extension;
    }

    public Path resolveStored(String relativePath) {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root.normalize())) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.INVALID_FILENAME, "非法存储路径");
        }
        return resolved;
    }

    public Path stage(byte[] bytes) {
        try {
            Path stagingDir = root.resolve(STAGING_SUBDIR);
            Files.createDirectories(stagingDir);
            Path staged = stagingDir.resolve(UUID.randomUUID() + ".part");
            Files.write(staged, bytes);
            return staged;
        } catch (IOException exception) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.STAGING_FAILED, "文件暂存失败");
        }
    }

    public void moveToSources(long userId, String sha256, String extension, Path staged) {
        try {
            Path sourceDir = root.resolve(SOURCES_SUBDIR).resolve(String.valueOf(userId));
            Files.createDirectories(sourceDir);
            Path target = sourceDir.resolve(sha256 + "." + extension);
            try {
                Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(staged, target);
            }
        } catch (IOException exception) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.COPY_FAILED, "文件复制失败");
        }
    }

    public void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 尽力清理，失败不影响主流程
        }
    }
}
