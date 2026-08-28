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
        boolean retryable,
        /** AI 失败或输出校验失败时供前端展示的可理解错误（成功时为空）。 */
        String errorCode,
        String errorMessage
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
                int structure,
                int evidence,
                int accuracy
        ) {
            public ScoreDetail(int clarity, int relevance, int depth, int accuracy) {
                this(clarity, relevance, depth, 0, accuracy, accuracy);
            }
        }
    }
}
