package com.resumego.schedule;

import java.time.LocalDateTime;

public record ScheduleEvent(
        Long id,
        Long userId,
        String title,
        String eventType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String notes,
        Long jobDescriptionId,
        Long jobProjectId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
