package com.resumego.interview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 创建面试会话请求。
 */
public record StartInterviewRequest(
        @NotNull(message = "简历版本 ID 不能为空")
        @Positive(message = "简历版本 ID 必须为正整数")
        Long resumeVersionId,

        @NotNull(message = "岗位 ID 不能为空")
        @Positive(message = "岗位 ID 必须为正整数")
        Long jobDescriptionId,

        @NotNull(message = "题目数量不能为空")
        @Min(value = 3, message = "题目数量最少为 3 道")
        @Max(value = 10, message = "题目数量最多为 10 道")
        Integer questionCount,

        @NotNull(message = "面试官人设 ID 不能为空")
        @Positive(message = "面试官人设 ID 必须为正整数")
        Long personaId
) {
}