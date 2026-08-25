package com.resumego.interview.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 模拟面试计划响应。mode 与开始上下文快照创建后不可变；
 * 历史回放展示快照中的名称与版本号，不用当前数据覆盖。
 */
public record InterviewPlanResponse(
        Long planId,
        String mode,
        String contextContractVersion,
        Map<String, Object> startContextSnapshot,
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
