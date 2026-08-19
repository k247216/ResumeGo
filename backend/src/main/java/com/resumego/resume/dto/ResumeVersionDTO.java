package com.resumego.resume.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ResumeVersionDTO(
        Long id,
        Long resumeId,
        Long parentVersionId,
        Integer versionNo,
        Map<String, Object> content,
        String changeSummary,
        String createdByType,
        LocalDateTime createdAt
) {
}
