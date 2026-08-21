package com.resumego.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateScheduleEventRequest(
        @NotBlank(message = "日程标题不能为空")
        @Size(max = 120, message = "日程标题不能超过 120 个字符")
        String title,
        String eventType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        @Size(max = 1000, message = "备注不能超过 1000 个字符")
        String notes,
        Long jobDescriptionId
) {
}
