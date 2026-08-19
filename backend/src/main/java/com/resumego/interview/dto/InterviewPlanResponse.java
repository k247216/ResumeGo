package com.resumego.interview.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 多轮模拟面试计划响应。
 */
public record InterviewPlanResponse(
        Long planId,
        Long resumeVersionId,
        Long jobDescriptionId,
        String title,
        Integer questionCount,
        List<String> focusTags,
        String supplement,
        MultiSessionSummaryResponse summary,
        LocalDateTime summaryGeneratedAt,
        List<Round> rounds,
        boolean completed,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record Round(
            Long sessionId,
            Long personaId,
            String personaName,
            String personaTitle,
            Integer roundOrder,
            String status,
            int currentQuestionIndex,
            int totalQuestions,
            boolean completed
    ) {
    }
}
