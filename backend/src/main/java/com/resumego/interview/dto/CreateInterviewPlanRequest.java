package com.resumego.interview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建一次多轮模拟面试计划。
 */
public record CreateInterviewPlanRequest(
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

        @NotEmpty(message = "至少选择一位面试官")
        @Size(max = 5, message = "面试官数量最多为 5 位")
        List<@Positive(message = "面试官人设 ID 必须为正整数") Long> personaIds,

        List<String> focusTags,

        String supplement
) {
}
