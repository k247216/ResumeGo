package com.resumego.schedule.dto;

import java.time.LocalDateTime;

public record ScheduleEventResponse(
        Long id,
        String title,
        String eventType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String notes,
        Long jobDescriptionId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
