package com.resumego.resume.dto;

import java.time.LocalDateTime;

public record ResumeDTO(
        Long id,
        String title,
        String kind,
        Long forkedFromVersionId,
        LocalDateTime archivedAt,
        Long targetJobDescriptionId,
        ResumeVersionDTO currentVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
