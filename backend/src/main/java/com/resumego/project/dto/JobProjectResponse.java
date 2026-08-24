package com.resumego.project.dto;

import java.time.LocalDateTime;

public record JobProjectResponse(
        Long id,
        String name,
        String status,
        String stage,
        Long jobDescriptionId,
        Long resumeVersionId,
        LocalDateTime archivedAt,
        LocalDateTime stageUpdatedAt,
        String industry,
        String targetRole,
        String location,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
