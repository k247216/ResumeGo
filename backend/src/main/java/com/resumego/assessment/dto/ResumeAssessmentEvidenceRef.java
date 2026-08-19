package com.resumego.assessment.dto;

public record ResumeAssessmentEvidenceRef(
        Long id,
        Long evidenceId,
        String sectionKey,
        boolean active
) {
}
