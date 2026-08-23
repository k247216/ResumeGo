package com.resumego.knowledge;

import java.util.Locale;
import java.util.Set;

/** 统一文件类型识别：扩展名小写归一；md/txt/docx 可解析提取正文，pdf/doc 仅收录元数据，其余为 unknown（禁止伪装 NOTE）。 */
public final class KnowledgeFileTypes {

    private static final Set<String> PARSEABLE = Set.of("md", "txt", "docx");
    private static final Set<String> METADATA_ONLY = Set.of("pdf", "doc");
    public static final String UNKNOWN = "unknown";

    private KnowledgeFileTypes() {
    }

    /** 扩展名小写归一（去前导点）；空/缺失 → unknown。 */
    public static String normalizeExtension(String raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith(".")) {
            lower = lower.substring(1);
        }
        return lower.isEmpty() ? UNKNOWN : lower;
    }

    public static boolean isParseable(String extension) {
        return PARSEABLE.contains(extension);
    }

    /** 已知但仅元数据的类型（当前不提取正文、不可编辑）。 */
    public static boolean isMetadataOnly(String extension) {
        return METADATA_ONLY.contains(extension);
    }

    /** 服务端判定真实 mediaType（不信任客户端 MIME）。 */
    public static String mediaTypeOf(String extension) {
        return switch (extension) {
            case "md" -> "text/markdown";
            case "txt" -> "text/plain";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }
}
