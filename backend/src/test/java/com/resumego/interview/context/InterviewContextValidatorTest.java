package com.resumego.interview.context;

import com.resumego.common.CurrentUser;
import com.resumego.interview.InterviewMode;
import com.resumego.interview.entity.InterviewerPersona;
import com.resumego.interview.mapper.InterviewerPersonaMapper;
import com.resumego.interview.repository.InterviewQuestionSetRepository;
import com.resumego.knowledge.KnowledgeDocument;
import com.resumego.knowledge.KnowledgeRepository;
import com.resumego.project.JobProject;
import com.resumego.project.JobProjectRepository;
import com.resumego.resume.dto.ResumeVersionDTO;
import com.resumego.resume.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("三模式开始上下文校验器")
class InterviewContextValidatorTest {

    @Mock
    private JobProjectRepository jobProjectRepository;
    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private InterviewerPersonaMapper personaMapper;
    @Mock
    private KnowledgeRepository knowledgeRepository;
    @Mock
    private InterviewQuestionSetRepository questionSetRepository;

    private RoleBasedContextValidator roleValidator;
    private KnowledgeTrainingContextValidator knowledgeValidator;
    private ExperienceSimulationContextValidator experienceValidator;

    @BeforeEach
    void setUp() {
        roleValidator = new RoleBasedContextValidator(jobProjectRepository, resumeRepository, personaMapper);
        knowledgeValidator = new KnowledgeTrainingContextValidator(knowledgeRepository);
        experienceValidator = new ExperienceSimulationContextValidator(questionSetRepository, personaMapper);
    }

    private JobProject project(Long id, Long jobDescriptionId) {
        return new JobProject(id, CurrentUser.DEMO_USER_ID, "腾讯 Java 后端", "active", "applied",
                jobDescriptionId, 10L, null, null, null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private ResumeVersionDTO version(long id, long resumeId, int versionNo) {
        return new ResumeVersionDTO(id, resumeId, null, versionNo, Map.of(), "创建", "user", LocalDateTime.now());
    }

    private InterviewerPersona persona(long id, String name) {
        InterviewerPersona persona = new InterviewerPersona();
        persona.setId(id);
        persona.setName(name);
        persona.setTitle("面试官");
        return persona;
    }

    @Test
    @DisplayName("岗位模式：合法输入产生含名称与版本号的最小快照")
    void roleBasedBuildsSnapshotWithNamesAndVersionNo() {
        when(jobProjectRepository.findById(CurrentUser.DEMO_USER_ID, 5L))
                .thenReturn(Optional.of(project(5L, 20L)));
        when(resumeRepository.findVersionByIdForUser(CurrentUser.DEMO_USER_ID, 10L))
                .thenReturn(version(10L, 1L, 3));
        when(resumeRepository.findTitleById(CurrentUser.DEMO_USER_ID, 1L)).thenReturn("后端简历");
        when(personaMapper.selectById(1L)).thenReturn(persona(1L, "技术面试官"));

        InterviewContextSnapshot snapshot = roleValidator.validate(new InterviewStartContext.RoleBased(
                5L, 10L, List.of(1L), 5, List.of("JVM"), null));

        assertThat(snapshot.mode()).isEqualTo("ROLE_BASED");
        assertThat(snapshot.jobProjectId()).isEqualTo(5L);
        assertThat(snapshot.jobProjectName()).isEqualTo("腾讯 Java 后端");
        assertThat(snapshot.resumeVersionId()).isEqualTo(10L);
        assertThat(snapshot.resumeTitle()).isEqualTo("后端简历");
        assertThat(snapshot.resumeVersionNo()).isEqualTo(3);
        assertThat(snapshot.jobDescriptionId()).isEqualTo(20L);
        assertThat(snapshot.personaIds()).containsExactly(1L);
        assertThat(snapshot.promptVersion()).isEqualTo("v1");
        // 快照不含正文
        assertThat(snapshot.toString()).doesNotContain("content");
    }

    @Test
    @DisplayName("岗位模式：缺求职目标/简历版本/人设/JD 均被阻止")
    void roleBasedRejectsMissingRequiredInputs() {
        when(jobProjectRepository.findById(CurrentUser.DEMO_USER_ID, 5L))
                .thenReturn(Optional.of(project(5L, 20L)));
        when(resumeRepository.findVersionByIdForUser(CurrentUser.DEMO_USER_ID, 10L))
                .thenReturn(version(10L, 1L, 2));

        assertThatThrownBy(() -> roleValidator.validate(new InterviewStartContext.RoleBased(
                null, 10L, List.of(1L), 5, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("求职目标");
        assertThatThrownBy(() -> roleValidator.validate(new InterviewStartContext.RoleBased(
                5L, null, List.of(1L), 5, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("简历版本");
        assertThatThrownBy(() -> roleValidator.validate(new InterviewStartContext.RoleBased(
                5L, 10L, List.of(), 5, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("面试官");
        assertThatThrownBy(() -> roleValidator.validate(new InterviewStartContext.RoleBased(
                5L, 10L, null, 5, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("面试官");

        when(jobProjectRepository.findById(CurrentUser.DEMO_USER_ID, 5L))
                .thenReturn(Optional.of(project(5L, null)));
        assertThatThrownBy(() -> roleValidator.validate(new InterviewStartContext.RoleBased(
                5L, 10L, List.of(1L), 5, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JD");
    }

    @Test
    @DisplayName("岗位模式：跨用户求职目标/简历版本/人设按不存在处理")
    void roleBasedRejectsCrossUserReferences() {
        when(jobProjectRepository.findById(CurrentUser.DEMO_USER_ID, 999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roleValidator.validate(new InterviewStartContext.RoleBased(
                999L, 10L, List.of(1L), 5, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("求职目标不存在");

        when(jobProjectRepository.findById(CurrentUser.DEMO_USER_ID, 5L))
                .thenReturn(Optional.of(project(5L, 20L)));
        when(resumeRepository.findVersionByIdForUser(anyLong(), anyLong())).thenReturn(null);
        assertThatThrownBy(() -> roleValidator.validate(new InterviewStartContext.RoleBased(
                5L, 999L, List.of(1L), 5, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("简历版本不存在");

        when(resumeRepository.findVersionByIdForUser(CurrentUser.DEMO_USER_ID, 10L))
                .thenReturn(version(10L, 1L, 1));
        when(personaMapper.selectById(999L)).thenReturn(null);
        assertThatThrownBy(() -> roleValidator.validate(new InterviewStartContext.RoleBased(
                5L, 10L, List.of(999L), 5, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("面试官人设不存在");
    }

    @Test
    @DisplayName("知识训练：合法资料产生标题快照；缺资料或跨用户被拒绝")
    void knowledgeTrainingValidatesDocuments() {
        when(knowledgeRepository.findById(CurrentUser.DEMO_USER_ID, 30L))
                .thenReturn(Optional.of(new KnowledgeDocument(30L, 1L, "JVM 笔记", "NOTE", "READY",
                        LocalDateTime.now(), LocalDateTime.now())));

        InterviewContextSnapshot snapshot = knowledgeValidator.validate(new InterviewStartContext.KnowledgeTraining(
                List.of(30L), "深入", "案例型", 5, List.of("GC"), null));

        assertThat(snapshot.mode()).isEqualTo("KNOWLEDGE_TRAINING");
        assertThat(snapshot.knowledgeDocumentIds()).containsExactly(30L);
        assertThat(snapshot.knowledgeDocumentTitles()).containsExactly("JVM 笔记");
        assertThat(snapshot.difficulty()).isEqualTo("深入");
        assertThat(snapshot.questionStyle()).isEqualTo("案例型");
        assertThat(snapshot.resumeVersionId()).isNull();

        assertThatThrownBy(() -> knowledgeValidator.validate(new InterviewStartContext.KnowledgeTraining(
                List.of(), null, 5, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("至少选择一份资料");

        when(knowledgeRepository.findById(CurrentUser.DEMO_USER_ID, 999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> knowledgeValidator.validate(new InterviewStartContext.KnowledgeTraining(
                List.of(999L), null, 5, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("知识资料不存在");
    }

    @Test
    @DisplayName("面经模拟：合法题集产生来源类型快照；归档题集被阻止")
    void experienceSimulationValidatesQuestionSets() {
        when(questionSetRepository.findSetById(CurrentUser.DEMO_USER_ID, 40L))
                .thenReturn(new InterviewQuestionSetRepository.QuestionSetRow(
                        40L, "腾讯面经", com.resumego.interview.QuestionSourceType.IMPORTED_EXPERIENCE, "牛客", false, null, null, null));
        when(questionSetRepository.findQuestionTexts(40L))
                .thenReturn(List.of("讲讲项目", "如何定位问题", "如何做技术取舍", "如何验证结果", "如何复盘", "如何处理冲突", "如何设计缓存", "如何保证一致性", "如何压测", "如何发布", "如何监控", "如何回滚"));
        when(personaMapper.selectById(1L)).thenReturn(persona(1L, "技术面试官"));

        InterviewContextSnapshot snapshot = experienceValidator.validate(new InterviewStartContext.ExperienceSimulation(
                40L, List.of(1L), "适中", 5, null, null));

        assertThat(snapshot.mode()).isEqualTo("EXPERIENCE_SIMULATION");
        assertThat(snapshot.questionSetId()).isEqualTo(40L);
        assertThat(snapshot.questionSetTitle()).isEqualTo("腾讯面经");
        assertThat(snapshot.questionSetSourceType()).isEqualTo("IMPORTED_EXPERIENCE");
        assertThat(snapshot.questionCount()).isEqualTo(5);

        InterviewContextSnapshot ordered = experienceValidator.validate(new InterviewStartContext.ExperienceSimulation(
                40L, List.of(1L), List.of(2, 0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11),
                "适中", "END_OF_SESSION", 5, null, null));
        assertThat(ordered.questionOrder()).containsExactly(2, 0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11);
        assertThatThrownBy(() -> experienceValidator.validate(new InterviewStartContext.ExperienceSimulation(
                40L, null, List.of(0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), "适中", null, 3, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("完整排列");

        assertThatThrownBy(() -> experienceValidator.validate(new InterviewStartContext.ExperienceSimulation(
                40L, null, null, 13, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("只有 12 道题");

        InterviewContextSnapshot defaulted = experienceValidator.validate(new InterviewStartContext.ExperienceSimulation(
                40L, null, null, null, null, null));
        assertThat(defaulted.questionCount()).isEqualTo(10);

        when(questionSetRepository.findSetById(CurrentUser.DEMO_USER_ID, 41L))
                .thenReturn(new InterviewQuestionSetRepository.QuestionSetRow(
                        41L, "旧题集", com.resumego.interview.QuestionSourceType.USER_MANUAL, null, true, null, null, null));
        assertThatThrownBy(() -> experienceValidator.validate(new InterviewStartContext.ExperienceSimulation(
                41L, null, null, 5, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已归档");

        assertThatThrownBy(() -> experienceValidator.validate(new InterviewStartContext.ExperienceSimulation(
                null, null, null, 5, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("题集");
    }

    @Test
    @DisplayName("supports 按模式分发，不互相越权")
    void validatorsSupportOnlyTheirMode() {
        assertThat(roleValidator.supports(InterviewMode.ROLE_BASED)).isTrue();
        assertThat(roleValidator.supports(InterviewMode.KNOWLEDGE_TRAINING)).isFalse();
        assertThat(knowledgeValidator.supports(InterviewMode.KNOWLEDGE_TRAINING)).isTrue();
        assertThat(knowledgeValidator.supports(InterviewMode.EXPERIENCE_SIMULATION)).isFalse();
        assertThat(experienceValidator.supports(InterviewMode.EXPERIENCE_SIMULATION)).isTrue();
        assertThat(experienceValidator.supports(InterviewMode.ROLE_BASED)).isFalse();
    }
}
