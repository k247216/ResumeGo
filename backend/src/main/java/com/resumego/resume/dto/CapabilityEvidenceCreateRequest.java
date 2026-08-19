package com.resumego.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CapabilityEvidenceCreateRequest(
        @NotBlank(message = "经历类型不能为空")
        String evidenceType,

        @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题不能超过 200 个字符")
        String title,

        String situation,

        @NotBlank(message = "行动描述不能为空")
        String actionText,

        String resultText,

        @NotEmpty(message = "技能标签至少填写一个")
        List<@NotBlank(message = "技能标签不能为空") String> skillTags,

        @Size(max = 500, message = "来源说明不能超过 500 个字符")
        String sourceNote
) {
}
