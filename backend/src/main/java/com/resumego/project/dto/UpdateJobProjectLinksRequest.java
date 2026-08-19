package com.resumego.project.dto;

public record UpdateJobProjectLinksRequest(
        Long jobDescriptionId,
        Long resumeVersionId
) {
}
