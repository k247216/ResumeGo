package com.resumego.interview.dto;

import com.resumego.interview.QuestionSourceType;

import java.util.List;

/** 创建或整体更新面经题集：元数据 + 有序题目。 */
public record InterviewQuestionSetRequest(
        String title,
        QuestionSourceType sourceType,
        String sourceNote,
        List<String> questions
) {
}
