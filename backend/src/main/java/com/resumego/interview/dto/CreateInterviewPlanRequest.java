package com.resumego.interview.dto;

import com.resumego.interview.InterviewMode;
import com.resumego.interview.context.InterviewStartContext;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建一次模拟面试计划。请求按 mode 键控：Controller/Service 先转换为明确的模式上下文，
 * 不允许全可选大 DTO 直接进入 Service；混入其他模式字段直接拒绝。
 */
public record CreateInterviewPlanRequest(
        InterviewMode mode,

        // ── ROLE_BASED ──
        @Positive(message = "求职目标 ID 必须为正整数")
        Long jobProjectId,
        @Positive(message = "简历版本 ID 必须为正整数")
        Long resumeVersionId,

        // ── KNOWLEDGE_TRAINING ──
        List<@Positive(message = "知识资料 ID 必须为正整数") Long> knowledgeDocumentIds,
        String difficulty,

        // ── EXPERIENCE_SIMULATION ──
        @Positive(message = "题集 ID 必须为正整数")
        Long questionSetId,
        String followUpIntensity,

        // ── 共享 ──
        @Min(value = 3, message = "题目数量最少为 3 道")
        @Max(value = 10, message = "题目数量最多为 10 道")
        Integer questionCount,

        @Size(max = 5, message = "面试官数量最多为 5 位")
        List<@Positive(message = "面试官人设 ID 必须为正整数") Long> personaIds,

        List<String> focusTags,

        String supplement
) {
    public CreateInterviewPlanRequest {
        if (mode == null) {
            mode = InterviewMode.ROLE_BASED;
        }
    }

    /** 转换为明确的模式上下文；混入其他模式字段直接拒绝。 */
    public InterviewStartContext toContext() {
        return switch (mode) {
            case ROLE_BASED -> {
                requireAbsent(knowledgeDocumentIds, "岗位模拟不接受知识资料字段");
                requireAbsent(questionSetId, "岗位模拟不接受题集字段");
                requireAbsent(difficulty, "岗位模拟不接受难度字段");
                yield new InterviewStartContext.RoleBased(
                        jobProjectId, resumeVersionId, personaIds, questionCount, focusTags, supplement);
            }
            case KNOWLEDGE_TRAINING -> {
                requireAbsent(jobProjectId, "知识训练不接受求职目标字段");
                requireAbsent(resumeVersionId, "知识训练不接受简历版本字段");
                requireAbsent(questionSetId, "知识训练不接受题集字段");
                yield new InterviewStartContext.KnowledgeTraining(
                        knowledgeDocumentIds, difficulty, questionCount, focusTags, supplement);
            }
            case EXPERIENCE_SIMULATION -> {
                requireAbsent(jobProjectId, "面经模拟不接受求职目标字段");
                requireAbsent(resumeVersionId, "面经模拟不接受简历版本字段");
                requireAbsent(knowledgeDocumentIds, "面经模拟不接受知识资料字段");
                yield new InterviewStartContext.ExperienceSimulation(
                        questionSetId, personaIds, followUpIntensity, questionCount, focusTags, supplement);
            }
        };
    }

    private static void requireAbsent(Object value, String message) {
        boolean present = value instanceof List<?> list ? !list.isEmpty() : value != null;
        if (present) {
            throw new IllegalArgumentException(message);
        }
    }
}
