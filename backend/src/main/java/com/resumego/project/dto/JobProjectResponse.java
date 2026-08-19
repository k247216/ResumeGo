package com.resumego.project.dto;

import java.time.LocalDateTime;

public record JobProjectResponse(
        Long id,
        String name,
        String status,
        Long jobDescriptionId,
        Long resumeVersionId,
        LocalDateTime archivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
