package com.resumego.interview.dto;

import java.util.List;

/**
 * 跨会话总结响应。
 */
public record MultiSessionSummaryResponse(
        String overallSummary,
        int overallScore,
        List<String> crossStrengths,
        List<String> crossWeaknesses,
        List<String> suggestions,
        List<SessionBrief> sessions
) {
    public record SessionBrief(
            Long sessionId,
            String personaName,
            String personaTitle,
            int totalQuestions
    ) {
    }
}