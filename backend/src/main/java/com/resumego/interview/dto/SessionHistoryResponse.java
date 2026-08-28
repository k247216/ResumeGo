package com.resumego.interview.dto;

import java.time.LocalDateTime;
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
            EvaluationSummary evaluation,
            String source,
            String sourceReference,
            String provenanceLabel,
            LocalDateTime submittedAt
    ) {
        public HistoryItem(int questionIndex, String questionText, String questionType,
                           String answerText, EvaluationSummary evaluation) {
            this(questionIndex, questionText, questionType, answerText, evaluation, null, null, null, null);
        }

        /** 兼容旧调用方：没有回答时提交时间为空。 */
        public HistoryItem(int questionIndex, String questionText, String questionType,
                           String answerText, EvaluationSummary evaluation,
                           String source, String sourceReference, String provenanceLabel) {
            this(questionIndex, questionText, questionType, answerText, evaluation,
                    source, sourceReference, provenanceLabel, null);
        }
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
            int structure,
            int evidence,
            int accuracy
    ) {
        public ScoreDetail(int clarity, int relevance, int depth, int accuracy) {
            this(clarity, relevance, depth, 0, accuracy, accuracy);
        }
    }
}
