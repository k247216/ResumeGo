package com.resumego.interview.context;

import com.resumego.interview.InterviewMode;

import java.util.List;

/**
 * 面试开始上下文：按模式区分的显式请求类型。
 * Controller 请求必须先转换为一种明确上下文，不允许全可选大 DTO 直接进入 Service。
 */
public sealed interface InterviewStartContext permits
        InterviewStartContext.RoleBased,
        InterviewStartContext.KnowledgeTraining,
        InterviewStartContext.ExperienceSimulation {

    InterviewMode mode();

    /** 岗位模拟：jobProjectId 与 resumeVersionId 必填，至少一个 persona。 */
    record RoleBased(
            Long jobProjectId,
            Long resumeVersionId,
            List<Long> personaIds,
            Integer questionCount,
            List<String> focusTags,
            String supplement
    ) implements InterviewStartContext {
        @Override
        public InterviewMode mode() {
            return InterviewMode.ROLE_BASED;
        }
    }

    /** 知识训练：只要求当前用户的 Knowledge Document，不强制岗位/简历。 */
    record KnowledgeTraining(
            List<Long> knowledgeDocumentIds,
            String difficulty,
            Integer questionCount,
            List<String> focusTags,
            String supplement
    ) implements InterviewStartContext {
        @Override
        public InterviewMode mode() {
            return InterviewMode.KNOWLEDGE_TRAINING;
        }
    }

    /** 面经模拟：只使用本地题集；AI 追问单独标源。 */
    record ExperienceSimulation(
            Long questionSetId,
            List<Long> personaIds,
            String followUpIntensity,
            Integer questionCount,
            List<String> focusTags,
            String supplement
    ) implements InterviewStartContext {
        @Override
        public InterviewMode mode() {
            return InterviewMode.EXPERIENCE_SIMULATION;
        }
    }
}
