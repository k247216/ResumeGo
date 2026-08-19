package com.resumego.resume.dto;

import java.time.LocalDateTime;

public record ResumeDTO(
        Long id,
        String title,
        Long targetJobDescriptionId,
        ResumeVersionDTO currentVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
