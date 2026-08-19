package com.resumego.optimization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.assessment.dto.ResumeAssessmentResponse;
import com.resumego.matching.dto.MatchDetails;
import com.resumego.matching.dto.MatchResponse;
import com.resumego.optimization.dto.JobMatchResumeContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * SuggestionPromptBuilder 单元测试。
 * 覆盖所有入参校验、边界情况、异常路径和输出格式校验。
 */
class SuggestionPromptBuilderTest {

    private SuggestionPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        promptBuilder = new SuggestionPromptBuilder(objectMapper);
    }

    // ── buildSystemPrompt ──

    @Test
    @DisplayName("系统提示词非空且包含核心原则")
    void shouldBuildValidSystemPrompt() {
        String prompt = promptBuilder.buildSystemPrompt();
        assertThat(prompt).isNotNull();
        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("项目技术表达教练");
        assertThat(prompt).contains("不得编造");
        assertThat(prompt).contains("JSON Schema");
    }

    // ── buildUserMessage: null 入参校验 ──

    @Test
    @DisplayName("resumeContent 为 null 时抛出异常")
    void shouldRejectNullResumeContent() {
        assertThatThrownBy(() -> promptBuilder.buildUserMessage(null, Map.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resumeContent");
    }

    @Test
    @DisplayName("parsedJd 为 null 时抛出异常")
    void shouldRejectNullParsedJd() {
        JobMatchResumeContent content = new JobMatchResumeContent(Map.of(), List.of());
        assertThatThrownBy(() -> promptBuilder.buildUserMessage(content, null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parsedJd");
    }

    @Test
    @DisplayName("matchGaps 为 null 时抛出异常")
    void shouldRejectNullMatchGaps() {
        JobMatchResumeContent content = new JobMatchResumeContent(Map.of(), List.of());
        assertThatThrownBy(() -> promptBuilder.buildUserMessage(content, Map.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("matchGaps");
    }

    // ── buildUserMessage: 正常路径 ──

    @Test
    @DisplayName("正常构建用户消息包含所有必要段落")
    void shouldBuildValidUserMessage() {
        JobMatchResumeContent content = new JobMatchResumeContent(
                Map.of("name", "测试用户", "skills", List.of("Java", "Spring")),
                List.of()
        );
        Map<String, Object> jd = Map.of("title", "后端开发工程师", "requirements", List.of("3年Java经验"));
        List<Map<String, Object>> gaps = List.of(Map.of("field", "skills", "score", 60));

        String message = promptBuilder.buildUserMessage(content, jd, gaps);

        assertThat(message).isNotNull();
        assertThat(message).contains("=== 简历内容 ===");
        assertThat(message).contains("=== 岗位要求 ===");
        assertThat(message).contains("=== 匹配缺口 ===");
        assertThat(message).contains("=== 可用能力证据 ===");
        assertThat(message).contains("=== 输出格式要求 ===");
        assertThat(message).contains("测试用户");
        assertThat(message).contains("后端开发工程师");
        assertThat(message).contains("suggestions");
    }

    @Test
    @DisplayName("普通模式可注入公司偏好 Profile 作为建议上下文")
    void shouldIncludeCompanyProfileInMockModeMessage() {
        JobMatchResumeContent content = new JobMatchResumeContent(
                Map.of("projects", List.of("订单系统")),
                List.of()
        );
        Map<String, Object> companyProfile = Map.of(
                "companyName", "字节跳动",
                "sourceType", "experience_based",
                "sourceNote", "经验型偏好，仅供演示",
                "preferenceTags", List.of("业务结果", "快速迭代"),
                "writingStyle", "突出个人动作和结果指标",
                "resumeAdviceRules", List.of("补充量化结果")
        );

        String message = promptBuilder.buildUserMessageWithCompanyProfile(
                content,
                Map.of("title", "后端开发实习生"),
                List.of(Map.of("field", "projects")),
                companyProfile
        );

        assertThat(message).contains("=== 公司偏好 Profile（仅供表达建议参考） ===");
        assertThat(message).contains("字节跳动", "experience_based", "业务结果", "快速迭代");
        assertThat(message).contains("不得作为评分、排序、录用概率或事实断言依据");
        assertThat(message).contains("至少引用一个 preferenceTags、interviewFocus 或 resumeAdviceRules");
        assertThat(message).contains("字节偏项目深挖和迭代结果");
    }

    @Test
    @DisplayName("空证据时显示无可用证据提示")
    void shouldShowNoEvidenceWhenEmpty() {
        JobMatchResumeContent content = new JobMatchResumeContent(Map.of(), List.of());
        String message = promptBuilder.buildUserMessage(content, Map.of(), List.of());

        assertThat(message).contains("无可用证据");
    }

    @Test
    @DisplayName("有证据时正确格式化证据信息")
    void shouldFormatEvidenceCorrectly() {
        var evidence = new JobMatchResumeContent.CapabilityEvidenceInfo(
                1L, "project", "电商平台重构",
                "老系统性能差", "引入Spring Cloud微服务", "QPS提升3倍",
                List.of("Java", "Spring Cloud", "MySQL")
        );
        JobMatchResumeContent content = new JobMatchResumeContent(Map.of(), List.of(evidence));

        String message = promptBuilder.buildUserMessage(content, Map.of(), List.of());

        assertThat(message).contains("证据 1:");
        assertThat(message).contains("类型: project");
        assertThat(message).contains("标题: 电商平台重构");
        assertThat(message).contains("背景: 老系统性能差");
        assertThat(message).contains("行动: 引入Spring Cloud微服务");
        assertThat(message).contains("结果: QPS提升3倍");
        assertThat(message).contains("技能: Java, Spring Cloud, MySQL");
    }

    @Test
    @DisplayName("证据 skillTags 为 null 时不抛出 NPE")
    void shouldHandleNullSkillTags() {
        var evidence = new JobMatchResumeContent.CapabilityEvidenceInfo(
                1L, "project", "测试项目",
                null, "做了某事", null,
                null
        );
        JobMatchResumeContent content = new JobMatchResumeContent(Map.of(), List.of(evidence));

        assertDoesNotThrow(() -> {
            String message = promptBuilder.buildUserMessage(content, Map.of(), List.of());
            assertThat(message).doesNotContain("技能:");
        });
    }

    @Test
    @DisplayName("证据 skillTags 为空列表时不抛出异常")
    void shouldHandleEmptySkillTags() {
        var evidence = new JobMatchResumeContent.CapabilityEvidenceInfo(
                1L, "project", "测试项目",
                null, "做了某事", null,
                List.of()
        );
        JobMatchResumeContent content = new JobMatchResumeContent(Map.of(), List.of(evidence));

        assertDoesNotThrow(() -> {
            String message = promptBuilder.buildUserMessage(content, Map.of(), List.of());
            assertThat(message).doesNotContain("技能:");
        });
    }

    @Test
    @DisplayName("多条证据时分隔正确")
    void shouldSeparateMultipleEvidences() {
        var e1 = new JobMatchResumeContent.CapabilityEvidenceInfo(
                1L, "project", "项目A", null, "行动A", null, List.of("Java")
        );
        var e2 = new JobMatchResumeContent.CapabilityEvidenceInfo(
                2L, "project", "项目B", null, "行动B", null, List.of("Python")
        );
        JobMatchResumeContent content = new JobMatchResumeContent(Map.of(), List.of(e1, e2));

        String message = promptBuilder.buildUserMessage(content, Map.of(), List.of());

        assertThat(message).contains("证据 1:");
        assertThat(message).contains("证据 2:");
    }

    // ── buildUserMessage: 边界情况 ──

    @Test
    @DisplayName("简历内容为 null 时正常序列化")
    void shouldHandleNullResumeContentField() {
        JobMatchResumeContent content = new JobMatchResumeContent(null, List.of());
        String message = promptBuilder.buildUserMessage(content, Map.of(), List.of());

        assertThat(message).contains("null");
    }

    @Test
    @DisplayName("输出格式包含必需字段")
    void shouldContainRequiredOutputFields() {
        JobMatchResumeContent content = new JobMatchResumeContent(Map.of(), List.of());
        String message = promptBuilder.buildUserMessage(content, Map.of(), List.of());

        assertThat(message).contains("sectionKey");
        assertThat(message).contains("originalText");
        assertThat(message).contains("suggestedText");
        assertThat(message).contains("reason");
        assertThat(message).contains("targetRequirement");
        assertThat(message).contains("evidenceId");
        assertThat(message).contains("confidence");
    }

    // ── 真实数据模式 Prompt ──

    @Test
    @DisplayName("真实数据模式在匹配结果为空时应拒绝构建")
    void shouldRejectNullMatchResponseInRealMode() {
        JobMatchResumeContent content = new JobMatchResumeContent(Map.of(), List.of());
        assertThatThrownBy(() -> promptBuilder.buildUserMessage(content, Map.of(), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("matchResponse");
    }

    @Test
    @DisplayName("真实数据模式应展示匹配明细、评分数据和证据上下文")
    void shouldBuildRealModeMessageWithAssessment() {
        JobMatchResumeContent content = new JobMatchResumeContent(
                Map.of("summary", "完成 Java 项目"),
                List.of(new JobMatchResumeContent.CapabilityEvidenceInfo(
                        1L, "project", "订单系统", "课程项目", "实现 Redis 缓存", "响应更稳定", List.of("Java", "Redis")))
        );
        MatchDetails details = new MatchDetails()
                .setRequiredCoverage(80)
                .setPreferredCoverage(60)
                .setExperienceCoverage(70)
                .setMatchedItems(List.of("Java"))
                .setMissingItems(List.of("Docker"))
                .setDimensionScores(Map.of("skills", 80));
        ResumeAssessmentResponse assessment = new ResumeAssessmentResponse(
                1L, 10L, "v1", BigDecimal.valueOf(78), Map.of("completeness", 80),
                List.of(Map.of("code", "missing_metric")), "fingerprint", LocalDateTime.now());

        String message = promptBuilder.buildUserMessage(content, Map.of("title", "后端开发"),
                MatchResponse.of(100L, 76, details), assessment);

        assertThat(message).contains("=== 简历评分 ===", "总分: 78", "missing_metric");
        assertThat(message).contains("=== 岗位匹配结果 ===", "综合匹配度: 76/100", "必备技能覆盖率: 80%");
        assertThat(message).contains("已匹配项", "Java", "缺失项", "Docker", "订单系统");
    }

    @Test
    @DisplayName("真实数据模式可注入公司偏好 Profile 但不改变评分匹配上下文")
    void shouldIncludeCompanyProfileInRealModeMessage() {
        MatchDetails details = new MatchDetails()
                .setRequiredCoverage(80)
                .setPreferredCoverage(60)
                .setExperienceCoverage(70)
                .setMatchedItems(List.of("Java"))
                .setMissingItems(List.of("Docker"))
                .setDimensionScores(Map.of("skills", 80));

        String message = promptBuilder.buildUserMessage(
                new JobMatchResumeContent(Map.of("summary", "Java 后端项目"), List.of()),
                Map.of("title", "后端开发"),
                MatchResponse.of(100L, 76, details),
                null,
                Map.of(
                        "companyName", "腾讯",
                        "sourceType", "experience_based",
                        "preferenceTags", List.of("工程稳定性"),
                        "writingStyle", "突出稳定性和协作边界"
                )
        );

        assertThat(message).contains("=== 公司偏好 Profile（仅供表达建议参考） ===");
        assertThat(message).contains("腾讯", "工程稳定性", "综合匹配度: 76/100");
        assertThat(message).contains("不同公司应体现不同建议重心");
    }

    @Test
    @DisplayName("真实数据模式在无评分数据时仍应生成岗位匹配建议上下文")
    void shouldBuildRealModeMessageWithoutAssessment() {
        MatchDetails details = new MatchDetails()
                .setRequiredCoverage(0).setPreferredCoverage(0).setExperienceCoverage(0)
                .setMatchedItems(List.of()).setMissingItems(List.of()).setDimensionScores(Map.of());
        String message = promptBuilder.buildUserMessage(
                new JobMatchResumeContent(Map.of(), List.of()), Map.of(), MatchResponse.of(1L, 0, details), null);

        assertThat(message).contains("=== 岗位匹配结果 ===", "综合匹配度: 0/100");
        assertThat(message).doesNotContain("=== 简历评分 ===");
    }

    // ── PROMPT_VERSION ──

    @Test
    @DisplayName("Prompt 版本号非空")
    void shouldHaveNonEmptyPromptVersion() {
        assertThat(SuggestionPromptBuilder.PROMPT_VERSION).isNotNull();
        assertThat(SuggestionPromptBuilder.PROMPT_VERSION).isNotBlank();
    }
}
