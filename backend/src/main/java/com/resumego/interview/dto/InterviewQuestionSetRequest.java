package com.resumego.interview.dto;

import com.resumego.interview.QuestionSourceType;

import java.util.List;

/** 创建或整体更新面经题集：元数据 + 有序题目。 */
public record InterviewQuestionSetRequest(
        String title,
        QuestionSourceType sourceType,
        String sourceNote,
        String companyName,
        String targetRole,
        String companyIconKey,
        List<String> questions
) {
    /** 兼容旧客户端：未提供岗位上下文元数据。 */
    public InterviewQuestionSetRequest(String title, QuestionSourceType sourceType,
                                       String sourceNote, List<String> questions) {
        this(title, sourceType, sourceNote, null, null, null, questions);
    }
}
