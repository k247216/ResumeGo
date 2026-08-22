package com.resumego.knowledge;

import java.util.Locale;

/** 文件名解析：只从 multipart 文件名提取一个安全的扩展名，绝不信任路径部分。 */
public final class KnowledgeFileNames {

    public record ParsedFileName(String originalName, String extension) {
    }

    private KnowledgeFileNames() {
    }

    public static ParsedFileName parse(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.INVALID_FILENAME, "文件名不能为空");
        }
        String name = originalFilename.trim();
        if (name.contains("/") || name.contains("\\") || name.contains("..")) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.INVALID_FILENAME, "文件名包含非法路径字符");
        }
        for (int i = 0; i < name.length(); i++) {
            if (Character.isISOControl(name.charAt(i))) {
                throw new KnowledgeImportException(KnowledgeErrorCodes.INVALID_FILENAME, "文件名包含非法字符");
            }
        }
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.UNSUPPORTED_TYPE, "仅支持 .md/.txt 文件");
        }
        String extension = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!extension.equals("md") && !extension.equals("txt")) {
            throw new KnowledgeImportException(KnowledgeErrorCodes.UNSUPPORTED_TYPE, "仅支持 .md/.txt 文件");
        }
        String normalized = name.length() > 255 ? name.substring(name.length() - 255) : name;
        return new ParsedFileName(normalized, extension);
    }
}
