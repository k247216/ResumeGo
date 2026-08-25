package com.resumego.interview.context;

import com.resumego.common.CurrentUser;
import com.resumego.interview.InterviewMode;
import com.resumego.interview.entity.InterviewerPersona;
import com.resumego.interview.mapper.InterviewerPersonaMapper;
import com.resumego.project.JobProject;
import com.resumego.project.JobProjectRepository;
import com.resumego.resume.dto.ResumeVersionDTO;
import com.resumego.resume.repository.ResumeRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 岗位模拟校验：Pipeline（job_project）与 Resume Version 分别属于当前用户且显式选择；
 * JD 缺失时阻止开始；Pipeline 当前绑定版本只用于默认建议，不锁定选择。
 */
@Component
public class RoleBasedContextValidator implements InterviewContextValidator {

    private final JobProjectRepository jobProjectRepository;
    private final ResumeRepository resumeRepository;
    private final InterviewerPersonaMapper personaMapper;

    public RoleBasedContextValidator(JobProjectRepository jobProjectRepository,
                                     ResumeRepository resumeRepository,
                                     InterviewerPersonaMapper personaMapper) {
        this.jobProjectRepository = jobProjectRepository;
        this.resumeRepository = resumeRepository;
        this.personaMapper = personaMapper;
    }

    @Override
    public boolean supports(InterviewMode mode) {
        return mode == InterviewMode.ROLE_BASED;
    }

    @Override
    public InterviewContextSnapshot validate(InterviewStartContext context) {
        if (!(context instanceof InterviewStartContext.RoleBased role)) {
            throw new IllegalArgumentException("岗位模拟上下文类型不正确");
        }
        if (role.jobProjectId() == null) {
            throw new IllegalArgumentException("岗位模拟必须明确选择求职目标");
        }
        if (role.resumeVersionId() == null) {
            throw new IllegalArgumentException("岗位模拟必须明确选择简历版本");
        }

        JobProject project = jobProjectRepository.findById(CurrentUser.DEMO_USER_ID, role.jobProjectId())
                .orElseThrow(() -> new IllegalArgumentException("求职目标不存在"));
        if (project.jobDescriptionId() == null) {
            throw new IllegalArgumentException("该求职目标尚未录入岗位 JD，请先补充后再开始岗位模拟");
        }

        ResumeVersionDTO version = resumeRepository.findVersionByIdForUser(CurrentUser.DEMO_USER_ID, role.resumeVersionId());
        if (version == null) {
            throw new IllegalArgumentException("简历版本不存在");
        }
        String resumeTitle = resumeRepository.findTitleById(CurrentUser.DEMO_USER_ID, version.resumeId());

        List<InterviewerPersona> personas = loadPersonas(role.personaIds());

        return new InterviewContextSnapshot(
                InterviewContextSnapshot.CONTRACT_VERSION,
                InterviewMode.ROLE_BASED.name(),
                project.id(),
                project.name(),
                version.id(),
                resumeTitle,
                version.versionNo(),
                project.jobDescriptionId(),
                null,
                null,
                null,
                null,
                null,
                personas.stream().map(InterviewerPersona::getId).toList(),
                personas.stream().map(InterviewerPersona::getName).toList(),
                role.questionCount(),
                role.focusTags(),
                null,
                null,
                InterviewContextSnapshot.PROMPT_VERSION,
                InterviewContextSnapshot.OUTPUT_SCHEMA_VERSION
        );
    }

    private List<InterviewerPersona> loadPersonas(List<Long> personaIds) {
        if (personaIds == null || personaIds.isEmpty()) {
            throw new IllegalArgumentException("至少选择一位面试官");
        }
        if (personaIds.size() > 5) {
            throw new IllegalArgumentException("面试官数量最多为 5 位");
        }
        return personaIds.stream()
                .map(personaId -> {
                    InterviewerPersona persona = personaMapper.selectById(personaId);
                    if (persona == null) {
                        throw new IllegalArgumentException("面试官人设不存在: " + personaId);
                    }
                    return persona;
                })
                .toList();
    }
}
