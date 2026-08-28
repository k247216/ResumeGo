package com.resumego.interview.source;

import com.resumego.interview.QuestionSourceType;
import com.resumego.interview.context.InterviewContextSnapshot;
import com.resumego.interview.repository.InterviewQuestionSetRepository;
import com.resumego.ai.AiClient;
import com.resumego.ai.AiClientSelector;
import com.resumego.ai.AiErrorCategory;
import com.resumego.ai.AiResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/** 三种题目来源适配器测试：岗位不越权、知识带引用、面经按序原题。 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewQuestionSource 适配器")
class InterviewQuestionSourceTest {

    @Mock
    private InterviewQuestionSetRepository questionSetRepository;
    @Mock
    private AiClientSelector aiClientSelector;
    @Mock
    private AiClient aiClient;
    @Mock
    private com.resumego.knowledge.KnowledgeService knowledgeService;

    private ExperienceQuestionSource experienceSource;
    private KnowledgeTrainingQuestionSource knowledgeSource;
    private RoleBasedQuestionSource roleSource;

    @BeforeEach
    void setUp() {
        experienceSource = new ExperienceQuestionSource(questionSetRepository);
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        knowledgeSource = new KnowledgeTrainingQuestionSource(aiClientSelector,
                new com.resumego.interview.service.InterviewPromptBuilder(mapper), knowledgeService, mapper,
                new com.resumego.ai.validate.AiOutputValidator(mapper));
        roleSource = new RoleBasedQuestionSource();
    }

    private InterviewContextSnapshot experienceSnapshot(long setId, String sourceType) {
        return new InterviewContextSnapshot("1", "EXPERIENCE_SIMULATION",
                null, null, null, null, null, null, null, null,
                setId, "腾讯面经", sourceType,
                null, null, 5, null, null, null, "v1", "v1");
    }

    private InterviewContextSnapshot knowledgeSnapshot(List<Long> docIds) {
        return new InterviewContextSnapshot("1", "KNOWLEDGE_TRAINING",
                null, null, null, null, null, null,
                docIds, List.of("JVM 笔记"), null, null, null,
                null, null, 5, null, "深入", null, "v1", "v1");
    }

    @Test
    @DisplayName("面经来源：按题集顺序返回原题，导入面经标注真实题目")
    void experienceReturnsOriginalQuestionsInOrder() {
        when(questionSetRepository.findQuestionTexts(40L)).thenReturn(List.of(
                "讲讲 JVM 内存结构", "Redis 持久化", "MySQL 索引"));

        List<QuestionDraft> drafts = experienceSource.prepare(experienceSnapshot(40L, "IMPORTED_EXPERIENCE"), 5);

        // 题集只有 3 题：返回 3 题，不生成题冒充面经
        assertThat(drafts).hasSize(3);
        assertThat(drafts).allSatisfy(draft -> {
            assertThat(draft.sourceType()).isEqualTo(QuestionSourceType.IMPORTED_EXPERIENCE);
            assertThat(draft.provenanceLabel()).isEqualTo(ExperienceQuestionSource.LABEL_REAL_QUESTION);
            assertThat(draft.sourceReference()).startsWith("question_set:40");
        });
        assertThat(drafts.get(0).text()).isEqualTo("讲讲 JVM 内存结构");
        assertThat(drafts.get(1).text()).isEqualTo("Redis 持久化");
    }

    @Test
    @DisplayName("面经来源：使用开始时快照中的题目顺序")
    void experienceUsesSnapshotQuestionOrder() {
        when(questionSetRepository.findQuestionTexts(40L)).thenReturn(List.of(
                "第一题", "第二题", "第三题"));

        InterviewContextSnapshot snapshot = new InterviewContextSnapshot(
                "1", "EXPERIENCE_SIMULATION", null, null, null, null, null, null,
                null, null, 40L, "腾讯面经", "IMPORTED_EXPERIENCE", null, null,
                3, null, null, null, null, null, "v1", "v1", List.of(2, 0, 1));

        List<QuestionDraft> drafts = experienceSource.prepare(snapshot, 2);

        assertThat(drafts).extracting(QuestionDraft::text)
                .containsExactly("第三题", "第一题");
    }

    @Test
    @DisplayName("面经来源：练习题集显著标注练习题")
    void experienceMarksGeneratedPracticeSets() {
        when(questionSetRepository.findQuestionTexts(41L)).thenReturn(List.of("练习题一"));

        List<QuestionDraft> drafts = experienceSource.prepare(experienceSnapshot(41L, "GENERATED_PRACTICE"), 5);

        assertThat(drafts.get(0).provenanceLabel()).isEqualTo(ExperienceQuestionSource.LABEL_PRACTICE);
    }

    @Test
    @DisplayName("面经来源：缺题集被拒绝")
    void experienceRejectsMissingSet() {
        assertThatThrownBy(() -> experienceSource.prepare(experienceSnapshotWithNullSet(), 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("题集");
    }

    private InterviewContextSnapshot experienceSnapshotWithNullSet() {
        return new InterviewContextSnapshot("1", "EXPERIENCE_SIMULATION",
                null, null, null, null, null, null, null, null,
                null, null, null,
                null, null, 5, null, null, null, "v1", "v1");
    }

    @Test
    @DisplayName("知识来源：AI 题目带来源文档引用；无 quote 明确标注未找到依据")
    void knowledgeAttachesSourceReferences() {
        when(knowledgeService.getContent(30L))
                .thenReturn(new com.resumego.knowledge.dto.KnowledgeContentResponse(30L, "JVM 内存分为堆、栈、方法区。"));
        when(aiClientSelector.getClient()).thenReturn(aiClient);
        when(aiClient.invoke(any())).thenReturn(new AiResult(
                "req-1", true,
                "{\"questions\":["
                        + "{\"questionText\":\"JVM 堆和方法区的区别？\",\"questionType\":\"基础\",\"sourceDocumentId\":30,\"quote\":\"JVM 内存分为堆、栈、方法区\"},"
                        + "{\"questionText\":\"GC 调优怎么做？\",\"questionType\":\"深入\"}"
                        + "]}",
                10, 20, 5L, null, null));

        List<QuestionDraft> drafts = knowledgeSource.prepare(knowledgeSnapshot(List.of(30L)), 2);

        assertThat(drafts).hasSize(2);
        assertThat(drafts.get(0).sourceReference()).startsWith("knowledge_doc:30:");
        assertThat(drafts.get(0).provenanceLabel()).isEqualTo(KnowledgeTrainingQuestionSource.LABEL_SOURCED);
        // 无 quote：明确标注未找到依据，不伪造引用
        assertThat(drafts.get(1).sourceReference()).isNull();
        assertThat(drafts.get(1).provenanceLabel()).isEqualTo(KnowledgeTrainingQuestionSource.LABEL_NO_EVIDENCE);
    }

    @Test
    @DisplayName("知识来源：AI 输出引用未选文档按无依据处理")
    void knowledgeRejectsReferencesOutsideSelectedDocs() {
        when(knowledgeService.getContent(30L))
                .thenReturn(new com.resumego.knowledge.dto.KnowledgeContentResponse(30L, "内容"));
        when(aiClientSelector.getClient()).thenReturn(aiClient);
        when(aiClient.invoke(any())).thenReturn(new AiResult(
                "req-2", true,
                "{\"questions\":[{\"questionText\":\"问题\",\"questionType\":\"基础\",\"sourceDocumentId\":888,\"quote\":\"片段\"}]}",
                10, 20, 5L, null, null));

        List<QuestionDraft> drafts = knowledgeSource.prepare(knowledgeSnapshot(List.of(30L)), 1);

        assertThat(drafts.get(0).provenanceLabel()).isEqualTo(KnowledgeTrainingQuestionSource.LABEL_NO_EVIDENCE);
        assertThat(drafts.get(0).sourceReference()).isNull();
    }

    @Test
    @DisplayName("知识来源：兼容模型常见的 fenced JSON 响应")
    void knowledgeAcceptsFencedJson() {
        when(knowledgeService.getContent(30L))
                .thenReturn(new com.resumego.knowledge.dto.KnowledgeContentResponse(30L, "Redis 使用快照持久化。"));
        when(aiClientSelector.getClient()).thenReturn(aiClient);
        when(aiClient.invoke(any())).thenReturn(new AiResult(
                "req-fenced", true,
                "```json\n{\"questions\":[{\"questionText\":\"Redis 快照何时触发？\",\"questionType\":\"基础\",\"sourceDocumentId\":30,\"quote\":\"Redis 使用快照持久化\"}]}\n```",
                10, 20, 5L, null, null));

        List<QuestionDraft> drafts = knowledgeSource.prepare(knowledgeSnapshot(List.of(30L)), 1);

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).text()).contains("Redis");
        assertThat(drafts.get(0).provenanceLabel()).isEqualTo(KnowledgeTrainingQuestionSource.LABEL_SOURCED);
    }

    @Test
    @DisplayName("知识来源：AI 失败抛出稳定错误")
    void knowledgeFailsStablyWhenAiFails() {
        when(knowledgeService.getContent(30L))
                .thenReturn(new com.resumego.knowledge.dto.KnowledgeContentResponse(30L, "内容"));
        when(aiClientSelector.getClient()).thenReturn(aiClient);
        when(aiClient.invoke(any())).thenReturn(AiResult.failure("req-3", com.resumego.ai.AiErrorCategory.PROVIDER_ERROR, "超时", 100L));

        assertThatThrownBy(() -> knowledgeSource.prepare(knowledgeSnapshot(List.of(30L)), 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("知识训练题目生成失败");
    }

    @Test
    @DisplayName("知识训练未配置 AI 时返回明确的配置引导")
    void knowledgeReportsMissingAiConfiguration() {
        when(knowledgeService.getContent(30L))
                .thenReturn(new com.resumego.knowledge.dto.KnowledgeContentResponse(30L, "内容"));
        when(aiClientSelector.getClient()).thenReturn(aiClient);
        when(aiClient.invoke(any())).thenReturn(AiResult.failure(
                "req-not-configured", AiErrorCategory.NOT_CONFIGURED,
                "尚未配置 AI 模型服务", 0L));

        assertThatThrownBy(() -> knowledgeSource.prepare(knowledgeSnapshot(List.of(30L)), 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("尚未配置 AI 模型服务")
                .hasMessageContaining("设置页");
    }

    @Test
    @DisplayName("岗位来源：prepare 返回空，不改变状态机实时出题行为")
    void roleBasedKeepsLiveGeneration() {
        assertThat(roleSource.supports(com.resumego.interview.InterviewMode.ROLE_BASED)).isTrue();
        assertThat(roleSource.prepare(experienceSnapshot(0L, "USER_MANUAL"), 5)).isEmpty();
        assertThat(roleSource.prepare(knowledgeSnapshot(List.of(30L)), 5)).isEmpty();
    }

    @Test
    @DisplayName("supports 按模式分发")
    void sourcesSupportOnlyTheirMode() {
        assertThat(experienceSource.supports(com.resumego.interview.InterviewMode.EXPERIENCE_SIMULATION)).isTrue();
        assertThat(experienceSource.supports(com.resumego.interview.InterviewMode.ROLE_BASED)).isFalse();
        assertThat(knowledgeSource.supports(com.resumego.interview.InterviewMode.KNOWLEDGE_TRAINING)).isTrue();
        assertThat(knowledgeSource.supports(com.resumego.interview.InterviewMode.EXPERIENCE_SIMULATION)).isFalse();
    }
}
