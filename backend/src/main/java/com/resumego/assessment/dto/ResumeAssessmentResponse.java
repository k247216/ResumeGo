package com.resumego.assessment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ResumeAssessmentResponse(
        Long id,
        Long resumeVersionId,
        String ruleVersion,
        BigDecimal totalScore,
        Map<String, Object> dimensionScores,
        List<Map<String, Object>> deductions,
        String inputFingerprint,
        LocalDateTime createdAt
) {
}
