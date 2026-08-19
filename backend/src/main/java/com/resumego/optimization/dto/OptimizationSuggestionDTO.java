package com.resumego.optimization.dto;

import java.time.LocalDateTime;

/**
 * AI 优化建议 DTO，用于 API 返回。
 */
public record OptimizationSuggestionDTO(
        Long id,
        Long jobMatchId,
        Long resumeVersionId,
        Long evidenceId,
        String sectionKey,
        String originalText,
        String suggestedText,
        String reasonText,
        String targetRequirement,
        String status,
        String riskLevel,
        LocalDateTime createdAt
) {
}
