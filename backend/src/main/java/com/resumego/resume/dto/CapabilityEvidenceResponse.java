package com.resumego.resume.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CapabilityEvidenceResponse(
        Long id,
        Long userId,
        String evidenceType,
        String title,
        String situation,
        String actionText,
        String resultText,
        List<String> skillTags,
        String sourceNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
