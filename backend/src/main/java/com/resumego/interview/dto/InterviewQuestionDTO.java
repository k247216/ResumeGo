package com.resumego.interview.dto;

/**
 * 面试问题展示 DTO。
 */
public record InterviewQuestionDTO(
        int questionIndex,
        String questionText,
        String questionType
) {
}