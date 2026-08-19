package com.resumego.assessment.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ResumeAssessmentResultDraft(
        BigDecimal totalScore,
        Map<String, Object> dimensionScores,
        List<Map<String, Object>> deductions
) {
}
