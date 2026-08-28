package com.resumego.interview.dto;

import java.util.List;

/**
 * 面试状态查询响应。
 */
public record InterviewStatusResponse(
        Long sessionId,
        String status,
        int currentQuestionIndex,
        int totalQuestions,
        InterviewQuestionDTO currentQuestion,
        String summaryJson,
        boolean completed,
        List<PerQuestionScore> perQuestionScores,
        String personaName,
        String personaTitle
) {
    /**
     * 每题得分。
     */
    public record PerQuestionScore(
            int questionIndex,
            String questionText,
            int clarity,
            int relevance,
            int depth,
            int structure,
            int evidence,
            int accuracy
    ) {
        public PerQuestionScore(int questionIndex, String questionText,
                                int clarity, int relevance, int depth, int accuracy) {
            this(questionIndex, questionText, clarity, relevance, depth, 0, accuracy, accuracy);
        }
    }
}
