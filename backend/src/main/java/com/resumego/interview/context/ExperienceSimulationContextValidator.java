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
                experience.questionCount(),
                experience.focusTags(),
                null,
                experience.followUpIntensity(),
                InterviewContextSnapshot.PROMPT_VERSION,
                InterviewContextSnapshot.OUTPUT_SCHEMA_VERSION
        );
    }
}
