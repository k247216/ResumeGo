package com.resumego.interview.dto;

/**
 * 知识库真实面经的无副作用格式预览。
 * READY 表示可物化；PROCESSING 表示正文尚未就绪；INVALID 表示已读取但不符合面经格式。
 */
public record InterviewQuestionSetSourcePreviewResponse(
        long documentId,
        String status,
        int questionCount,
        String message,
        String companyName,
        String targetRole,
        String companyIconKey
) {
    public static InterviewQuestionSetSourcePreviewResponse processing(long documentId, String message) {
        return new InterviewQuestionSetSourcePreviewResponse(documentId, "PROCESSING", 0, message, null, null, null);
    }

    public static InterviewQuestionSetSourcePreviewResponse invalid(long documentId, String message) {
        return new InterviewQuestionSetSourcePreviewResponse(documentId, "INVALID", 0, message, null, null, null);
    }
}
