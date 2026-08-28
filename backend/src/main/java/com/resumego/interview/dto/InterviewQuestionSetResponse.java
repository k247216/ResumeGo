package com.resumego.interview.dto;

import com.resumego.interview.QuestionSourceType;

import java.time.LocalDateTime;
import java.util.List;

/** 面经题集响应：列表模式不含题目正文（items 为 null），详情模式返回有序题目。 */
public record InterviewQuestionSetResponse(
        Long id,
        String title,
        QuestionSourceType sourceType,
        String sourceNote,
        String companyName,
        String targetRole,
        String companyIconKey,
        Long sourceDocumentId,
        Integer questionCount,
        boolean archived,
        LocalDateTime archivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<QuestionItem> items
) {
    /** 兼容历史响应构造：元数据为空，题数从详情题目推导。 */
    public InterviewQuestionSetResponse(Long id, String title, QuestionSourceType sourceType,
                                        String sourceNote, boolean archived,
                                        LocalDateTime archivedAt, LocalDateTime createdAt,
                                        LocalDateTime updatedAt, List<QuestionItem> items) {
        this(id, title, sourceType, sourceNote, null, null, null,
                null, items == null ? null : items.size(), archived, archivedAt, createdAt, updatedAt, items);
    }
    public record QuestionItem(
            int positionIndex,
            String questionText
    ) {
    }
}
