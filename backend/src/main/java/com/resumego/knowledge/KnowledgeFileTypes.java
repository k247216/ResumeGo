package com.resumego.knowledge;

import java.util.Locale;
import java.util.Set;

/** 统一文件类型识别：扩展名小写归一；md/txt/docx/pptx 可解析提取正文，pdf/doc 仅收录元数据，其余为 unknown（禁止伪装 NOTE）。 */
public final class KnowledgeFileTypes {

    private static final Set<String> PARSEABLE = Set.of("md", "txt", "docx", "pptx");
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


    /** 服务端判定真实 mediaType（不信任客户端 MIME；常见类型映射，未知归 octet-stream）。 */
    public static String mediaTypeOf(String extension) {
        return switch (extension) {
            case "md" -> "text/markdown";
            case "txt", "csv" -> "text/plain";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "bmp" -> "image/bmp";
            case "ico" -> "image/x-icon";
            case "json" -> "application/json";
            case "html", "htm" -> "text/html";
            case "xml" -> "application/xml";
            case "zip" -> "application/zip";
            case "mp3" -> "audio/mpeg";
            case "mp4" -> "video/mp4";
            default -> "application/octet-stream";
        };
    }
}
