package com.resumego.optimization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;

/**
 * POST /api/v1/resume-versions/{versionId}/ai-suggestions 请求体。
 */
public record GenerateSuggestionsRequest(
        @JsonProperty("jobDescriptionId")
        @Positive(message = "jobDescriptionId 必须为正整数")
        Long jobDescriptionId,

        @JsonProperty("assessmentId")
        @Positive(message = "assessmentId 必须为正整数")
        Long assessmentId,

        @JsonProperty("matchId")
        @Positive(message = "matchId 必须为正整数")
        Long matchId
) {}
