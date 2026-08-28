package com.resumego.interview.context;

import com.resumego.common.CurrentUser;
import com.resumego.interview.InterviewMode;
import com.resumego.interview.entity.InterviewerPersona;
import com.resumego.interview.mapper.InterviewerPersonaMapper;
import com.resumego.interview.repository.InterviewQuestionSetRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 面经模拟校验：只使用当前用户本地题集；归档题集不能开始新面试。
 */
@Component
public class ExperienceSimulationContextValidator implements InterviewContextValidator {

    private static final int MAX_PRACTICE_QUESTIONS = 30;

    private final InterviewQuestionSetRepository questionSetRepository;
    private final InterviewerPersonaMapper personaMapper;

    public ExperienceSimulationContextValidator(InterviewQuestionSetRepository questionSetRepository,
                                                InterviewerPersonaMapper personaMapper) {
        this.questionSetRepository = questionSetRepository;
        this.personaMapper = personaMapper;
    }

    @Override
    public boolean supports(InterviewMode mode) {
        return mode == InterviewMode.EXPERIENCE_SIMULATION;
    }

    @Override
    public InterviewContextSnapshot validate(InterviewStartContext context) {
        if (!(context instanceof InterviewStartContext.ExperienceSimulation experience)) {
            throw new IllegalArgumentException("面经模拟上下文类型不正确");
        }
        if (experience.questionSetId() == null) {
            throw new IllegalArgumentException("面经模拟必须选择本地题集");
        }

        InterviewQuestionSetRepository.QuestionSetRow set =
                questionSetRepository.findSetById(CurrentUser.DEMO_USER_ID, experience.questionSetId());
        if (set == null) {
            throw new IllegalArgumentException("面经题集不存在");
        }
        if (set.archived()) {
            throw new IllegalArgumentException("该题集已归档，不能开始新的面试");
        }

        List<String> sourceQuestions = questionSetRepository.findQuestionTexts(set.id());
        if (sourceQuestions.isEmpty()) {
            throw new IllegalArgumentException("所选题集没有可练习的原始题目");
        }
        List<Integer> questionOrder = validateQuestionOrder(experience.questionOrder(), sourceQuestions.size());
        int requestedCount = experience.questionCount() == null
                ? Math.min(10, sourceQuestions.size())
                : experience.questionCount();
        if (requestedCount < 1) {
            throw new IllegalArgumentException("面经演练至少需要 1 道题");
        }
        if (requestedCount > MAX_PRACTICE_QUESTIONS) {
            throw new IllegalArgumentException("面经演练单次最多练习 " + MAX_PRACTICE_QUESTIONS + " 道题");
        }
        if (requestedCount > sourceQuestions.size()) {
            throw new IllegalArgumentException("所选题集只有 " + sourceQuestions.size()
                    + " 道题，不能练习 " + requestedCount + " 道");
        }

        List<Long> personaIds = experience.personaIds() == null ? List.of() : experience.personaIds();
        List<String> personaNames = personaIds.stream()
                .map(personaId -> {
                    InterviewerPersona persona = personaMapper.selectById(personaId);
                    return persona == null ? null : persona.getName();
                })
                .toList();

        return new InterviewContextSnapshot(
                InterviewContextSnapshot.CONTRACT_VERSION,
                InterviewMode.EXPERIENCE_SIMULATION.name(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                set.id(),
                set.title(),
                set.sourceType().name(),
                personaIds,
                personaNames,
                requestedCount,
                experience.focusTags(),
                null,
                null,
                experience.followUpIntensity(),
                experience.reviewMode(),
                InterviewContextSnapshot.PROMPT_VERSION,
                InterviewContextSnapshot.OUTPUT_SCHEMA_VERSION,
                questionOrder
        );
    }

    /** 只接受完整排列，避免遗漏、重复或越界导致题目顺序与用户预览不一致。 */
    private List<Integer> validateQuestionOrder(List<Integer> order, int questionCount) {
        if (order == null || order.isEmpty()) return null;
        if (order.size() != questionCount) {
            throw new IllegalArgumentException("题目顺序必须包含题集中的全部 " + questionCount + " 道题");
        }
        boolean[] seen = new boolean[questionCount];
        for (Integer index : order) {
            if (index == null || index < 0 || index >= questionCount || seen[index]) {
                throw new IllegalArgumentException("题目顺序必须是从 0 开始且不重复的完整排列");
            }
            seen[index] = true;
        }
        return List.copyOf(order);
    }
}
