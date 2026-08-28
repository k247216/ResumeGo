package com.resumego.interview.dto;

/**
 * 面试问题展示 DTO。
 */
public record InterviewQuestionDTO(
        int questionIndex,
        String questionText,
        String questionType,
        String source,
        String sourceReference,
        String provenanceLabel
) {
    /** 保持旧调用方兼容：没有来源信息时按普通 AI 题目处理。 */
    public InterviewQuestionDTO(int questionIndex, String questionText, String questionType) {
        this(questionIndex, questionText, questionType, null, null, null);
    }
}
