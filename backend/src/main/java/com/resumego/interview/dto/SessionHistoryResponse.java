package com.resumego.interview.dto;

import java.util.List;

/**
 * 会话问答历史响应，用于前端恢复会话时加载全部问答记录。
 */
public record SessionHistoryResponse(
        Long sessionId,
        List<HistoryItem> items
) {
    public record HistoryItem(
            int questionIndex,
            String questionText,
            String questionType,
            String answerText,
            EvaluationSummary evaluation
    ) {
    }

    public record EvaluationSummary(
            List<String> strengths,
            List<String> weaknesses,
            List<String> suggestions,
            String referenceAnswer,
            ScoreDetail score
    ) {
    }

    public record ScoreDetail(
            int clarity,
            int relevance,
            int depth,
            int accuracy
    ) {
    }
}