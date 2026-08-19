package com.resumego.assessment.dto;

import java.util.List;
import java.util.Map;

public record ResumeAssessmentInput(
        Long resumeVersionId,
        Map<String, Object> content,
        List<ResumeAssessmentEvidenceRef> evidenceRefs
) {
}
