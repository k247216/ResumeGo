package com.resumego.project;

import java.time.LocalDateTime;

public record JobProject(
        Long id,
        Long userId,
        String name,
        String status,
        Long jobDescriptionId,
        Long resumeVersionId,
        LocalDateTime archivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
