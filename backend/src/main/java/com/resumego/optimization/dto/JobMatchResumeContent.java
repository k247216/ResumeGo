package com.resumego.optimization.dto;

import java.util.List;
import java.util.Map;

/**
 * 简历内容 DTO，用于构建 AI Prompt。
 */
public record JobMatchResumeContent(
        Map<String, Object> content,
        List<CapabilityEvidenceInfo> evidences
) {

    /**
     * 能力证据信息。
     */
    public record CapabilityEvidenceInfo(
            Long id,
            String type,
            String title,
            String situation,
            String actionText,
            String resultText,
            List<String> skillTags
    ) {}
}
