package com.resumego.project.dto;

import java.time.LocalDateTime;

public record StageEventResponse(
        Long id,
        String stage,
        LocalDateTime occurredAt
) {
}
