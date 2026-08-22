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


    /**
     * 读取受管文件准备替换：校验普通文件、非符号链接、realpath 仍在 data root 内；返回字节作为回滚备份。
     */
    public byte[] readManagedForReplace(long userId, String relativePath) {
        Path managed = resolveStored(relativePath);
        Path ownSourceDir = root.resolve("knowledge/sources").resolve(String.valueOf(userId)).normalize();
        if (!managed.startsWith(ownSourceDir)) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.INVALID_FILENAME, "受管文件不属于当前用户");
        }
        try {
            if (Files.isSymbolicLink(managed)) {
                throw new KnowledgeImportException(KnowledgeErrorCodes.INVALID_FILENAME, "受管文件不能是符号链接");
            }
            Path realRoot = root.toRealPath();
            Path real = managed.toRealPath();
            if (!real.startsWith(realRoot)) {
                throw new KnowledgeImportException(KnowledgeErrorCodes.INVALID_FILENAME, "真实路径越界");
            }
            if (!Files.isRegularFile(managed)) {
                throw new KnowledgeImportException(KnowledgeErrorCodes.INVALID_FILENAME, "受管目标不是普通文件");
            }
            return Files.readAllBytes(managed);
        } catch (IOException exception) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.EXTRACTION_FAILED, "读取受管文件失败");
        }
    }

    /** 准备替换：把新字节写入 staging 临时文件，返回其路径。 */
    public Path stageReplacement(byte[] bytes) {
        try {
            Path stagingDir = root.resolve(STAGING_SUBDIR);
            Files.createDirectories(stagingDir);
            Path staged = stagingDir.resolve(UUID.randomUUID() + ".part");
            Files.write(staged, bytes);
            return staged;
        } catch (IOException exception) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.STAGING_FAILED, "受管文件写入失败");
        }
    }

    /** 原子提交替换：staging 临时文件覆盖受管文件。 */
    public void commitReplacement(Path managed, Path staged) {
        try {
            Files.move(staged, managed, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
            try {
                Files.move(staged, managed, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e2) {
                throw new KnowledgeImportException(KnowledgeErrorCodes.COPY_FAILED, "受管文件替换失败");
            }
        } catch (IOException exception) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.COPY_FAILED, "受管文件替换失败");
        }
    }

    /** 数据库失败回滚：用备份字节恢复受管文件；提供 REPLACE_EXISTING fallback 并尽力清理 rollback 临时文件。 */
    public void restoreManaged(Path managed, byte[] oldBytes) {
        Path tmp = null;
        try {
            Files.createDirectories(managed.getParent());
            tmp = managed.resolveSibling(UUID.randomUUID() + ".rollback");
            Files.write(tmp, oldBytes);
            try {
                Files.move(tmp, managed, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(tmp, managed, StandardCopyOption.REPLACE_EXISTING);
            }
            tmp = null;
        } catch (IOException exception) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.EXTRACTION_FAILED, "受管文件恢复失败");
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                    // 尽力清理
                }
            }
        }
    }

    /** 计算 data dir 下的相对路径（用于 staging 记录）。 */
    public String relativePath(Path absolute) {
        return root.relativize(absolute.normalize()).toString().replace('\\', '/');
    }

    /**
     * 只允许删除 knowledge/staging/{name} 或 knowledge/sources/{userId}/{name}；
     * 路径越界或用户目录不匹配直接拒绝。
     */
    public void deleteManaged(long userId, String relativePath) {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root.normalize())) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.INVALID_FILENAME, "非法清理路径");
        }
        Path stagingDir = root.resolve("knowledge/staging").normalize();
        Path sourcesRoot = root.resolve("knowledge/sources").normalize();
        Path ownSourceDir = sourcesRoot.resolve(String.valueOf(userId)).normalize();
        boolean inStaging = resolved.startsWith(stagingDir);
        boolean inSources = resolved.startsWith(sourcesRoot);
        if (!inStaging && !inSources) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.INVALID_FILENAME, "清理路径越界");
        }
        if (inSources && !resolved.startsWith(ownSourceDir)) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.INVALID_FILENAME, "清理路径不属于当前用户");
        }
        try {
            Files.deleteIfExists(resolved);
        } catch (IOException exception) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.EXTRACTION_FAILED, "清理受管文件失败");
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
