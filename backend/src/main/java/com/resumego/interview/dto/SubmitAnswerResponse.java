package com.resumego.interview.dto;

import java.util.List;

/**
 * 提交回答响应。
 */
public record SubmitAnswerResponse(
        Long sessionId,
        String status,
        int currentQuestionIndex,
        int totalQuestions,
        InterviewQuestionDTO nextQuestion,
        EvaluationSummary evaluation,
        boolean completed,
        boolean retryable
) {
    /**
     * 评价摘要（提交回答后返回）。
     */
    public record EvaluationSummary(
            List<String> strengths,
            List<String> weaknesses,
            List<String> suggestions,
            String referenceAnswer,
            ScoreDetail score
    ) {
        /**
         * 得分明细。
         */
        public record ScoreDetail(
                int clarity,
                int relevance,
                int depth,
                int accuracy
        ) {
        }
    }
}