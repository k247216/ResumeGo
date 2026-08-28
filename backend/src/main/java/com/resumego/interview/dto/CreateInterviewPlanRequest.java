package com.resumego.interview.dto;

import com.resumego.interview.InterviewMode;
import com.resumego.interview.context.InterviewStartContext;
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
        String questionStyle,

        // ── EXPERIENCE_SIMULATION ──
        @Positive(message = "题集 ID 必须为正整数")
        Long questionSetId,
        String followUpIntensity,
        String reviewMode,
        List<Integer> questionOrder,

        // ── 共享 ──
        Integer questionCount,

        @Size(max = 5, message = "面试官数量最多为 5 位")
        List<@Positive(message = "面试官人设 ID 必须为正整数") Long> personaIds,

        List<String> focusTags,

        String supplement
) {
    private static final java.util.Set<String> REVIEW_MODES = java.util.Set.of(
            "PER_QUESTION", "END_OF_SESSION", "SOURCE_ONLY");
    /** 兼容旧客户端：未发送知识训练提问风格时保持 null。 */
    public CreateInterviewPlanRequest(
            InterviewMode mode,
            Long jobProjectId,
            Long resumeVersionId,
                List<Long> knowledgeDocumentIds,
                String difficulty,
                Long questionSetId,
                String followUpIntensity,
            Integer questionCount,
            List<Long> personaIds,
            List<String> focusTags,
            String supplement
        ) {
        this(mode, jobProjectId, resumeVersionId, knowledgeDocumentIds, difficulty, null,
                questionSetId, followUpIntensity, null, null, questionCount, personaIds, focusTags, supplement);
    }

    /** 兼容已支持答题回顾、但尚未支持题目排序的客户端。 */
    public CreateInterviewPlanRequest(
            InterviewMode mode,
            Long jobProjectId,
            Long resumeVersionId,
            List<Long> knowledgeDocumentIds,
            String difficulty,
            Long questionSetId,
            String followUpIntensity,
            String reviewMode,
            Integer questionCount,
            List<Long> personaIds,
            List<String> focusTags,
            String supplement
    ) {
        this(mode, jobProjectId, resumeVersionId, knowledgeDocumentIds, difficulty, null,
                questionSetId, followUpIntensity, reviewMode, null, questionCount,
                personaIds, focusTags, supplement);
    }

    /** 兼容同时携带知识训练提问风格与面经回顾设置的旧客户端。 */
    public CreateInterviewPlanRequest(
            InterviewMode mode,
            Long jobProjectId,
            Long resumeVersionId,
            List<Long> knowledgeDocumentIds,
            String difficulty,
            String questionStyle,
            Long questionSetId,
            String followUpIntensity,
            String reviewMode,
            Integer questionCount,
            List<Long> personaIds,
            List<String> focusTags,
            String supplement
    ) {
        this(mode, jobProjectId, resumeVersionId, knowledgeDocumentIds, difficulty, questionStyle,
                questionSetId, followUpIntensity, reviewMode, null, questionCount,
                personaIds, focusTags, supplement);
    }

    public CreateInterviewPlanRequest {
        if (mode == null) {
            mode = InterviewMode.ROLE_BASED;
        }
    }

    /** 转换为明确的模式上下文；混入其他模式字段直接拒绝。 */
    public InterviewStartContext toContext() {
        validateQuestionCount();
        return switch (mode) {
            case ROLE_BASED -> {
                requireAbsent(knowledgeDocumentIds, "岗位模拟不接受知识资料字段");
                requireAbsent(questionSetId, "岗位模拟不接受题集字段");
                requireAbsent(difficulty, "岗位模拟不接受难度字段");
                requireAbsent(questionStyle, "岗位模拟不接受提问风格字段");
                requireAbsent(reviewMode, "岗位模拟不接受答题回顾方式字段");
                requireAbsent(questionOrder, "岗位模拟不接受题目顺序字段");
                yield new InterviewStartContext.RoleBased(
                        jobProjectId, resumeVersionId, personaIds, questionCount, focusTags, supplement);
            }
            case KNOWLEDGE_TRAINING -> {
                requireAbsent(jobProjectId, "知识训练不接受求职目标字段");
                requireAbsent(resumeVersionId, "知识训练不接受简历版本字段");
                requireAbsent(questionSetId, "知识训练不接受题集字段");
                requireAbsent(reviewMode, "知识训练不接受答题回顾方式字段");
                requireAbsent(questionOrder, "知识训练不接受题目顺序字段");
                yield new InterviewStartContext.KnowledgeTraining(
                        knowledgeDocumentIds, difficulty, questionStyle, questionCount, focusTags, supplement);
            }
            case EXPERIENCE_SIMULATION -> {
                requireAbsent(jobProjectId, "面经模拟不接受求职目标字段");
                requireAbsent(resumeVersionId, "面经模拟不接受简历版本字段");
                requireAbsent(knowledgeDocumentIds, "面经模拟不接受知识资料字段");
                requireAbsent(questionStyle, "面经模拟不接受提问风格字段");
                if (reviewMode != null && !REVIEW_MODES.contains(reviewMode)) {
                    throw new IllegalArgumentException("不支持的答题回顾方式: " + reviewMode);
                }
                yield new InterviewStartContext.ExperienceSimulation(
                        questionSetId, personaIds, questionOrder, followUpIntensity, reviewMode,
                        questionCount, focusTags, supplement);
            }
        };
    }

    private void validateQuestionCount() {
        if (questionCount == null) {
            return;
        }
        String message = switch (mode) {
            case ROLE_BASED -> questionCount < 5 || questionCount > 15
                    ? "岗位模拟题目数量应为 5-15 道" : null;
            case KNOWLEDGE_TRAINING -> questionCount < 1 || questionCount > 20
                    ? "知识训练题目数量应为 1-20 道" : null;
            case EXPERIENCE_SIMULATION -> questionCount < 1 || questionCount > 30
                    ? "面经演练题目数量应为 1-30 道" : null;
        };
        if (message != null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireAbsent(Object value, String message) {
        boolean present = value instanceof List<?> list ? !list.isEmpty() : value != null;
        if (present) {
            throw new IllegalArgumentException(message);
        }
    }
}
