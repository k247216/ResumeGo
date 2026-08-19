package com.resumego.interview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * InterviewPromptBuilder 单元测试。
 * 覆盖所有 Prompt 构建方法、输出 Schema 格式和边界情况。
 */
class InterviewPromptBuilderTest {

    private InterviewPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        promptBuilder = new InterviewPromptBuilder(objectMapper);
    }

    // ── 问题生成 Prompt ──

    @Test
    @DisplayName("问题系统提示词非空且包含核心原则")
    void shouldBuildQuestionSystemPrompt() {
        String prompt = promptBuilder.buildQuestionSystemPrompt(Map.of());

        assertThat(prompt).isNotNull();
        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("模拟面试官");
        assertThat(prompt).contains("简历内容");
        assertThat(prompt).contains("岗位要求");
        assertThat(prompt).contains("JSON Schema");
    }

    @Test
    @DisplayName("问题用户消息包含所有必要段落")
    void shouldBuildQuestionUserMessage() {
        Map<String, Object> resume = Map.of("name", "张三", "skills", List.of("Java"));
        Map<String, Object> jd = Map.of("title", "后端开发");
        List<Map<String, String>> previous = List.of();

        String message = promptBuilder.buildQuestionUserMessage(resume, jd, 1, 3, previous);

        assertThat(message).contains("=== 候选人简历 ===");
        assertThat(message).contains("=== 目标岗位要求 ===");
        assertThat(message).contains("=== 输出格式要求 ===");
        assertThat(message).contains("questionText");
        assertThat(message).contains("questionType");
        assertThat(message).contains("targetSkill");
        assertThat(message).contains("第 1/3");
    }

    @Test
    @DisplayName("问题用户消息应融合公司 Profile 但不控制状态机")
    void shouldIncludeCompanyProfileInQuestionPrompt() {
        String message = promptBuilder.buildQuestionUserMessage(
                Map.of("projects", List.of("订单系统")),
                Map.of("title", "后端开发"),
                2,
                5,
                List.of(),
                companyProfile("字节跳动")
        );

        assertThat(message).contains("=== 公司偏好 Profile（仅供面试问题生成参考） ===");
        assertThat(message).contains("字节跳动", "项目深挖", "快速迭代");
        assertThat(message).contains("不得用于控制面试状态、题号、结束条件、分数规则");
    }

    @Test
    @DisplayName("问题用户消息包含已问过的问题")
    void shouldIncludePreviousQuestions() {
        Map<String, Object> resume = Map.of();
        Map<String, Object> jd = Map.of();
        List<Map<String, String>> previous = List.of(
                Map.of("questionText", "请介绍你的项目", "questionType", "project_experience"),
                Map.of("questionText", "你熟悉哪些技术栈", "questionType", "technical_skill")
        );

        String message = promptBuilder.buildQuestionUserMessage(resume, jd, 3, 3, previous);

        assertThat(message).contains("=== 已问过的问题 ===");
        assertThat(message).contains("请介绍你的项目");
        assertThat(message).contains("你熟悉哪些技术栈");
        assertThat(message).contains("第 3/3");
    }

    @Test
    @DisplayName("问题用户消息 previousQuestions 为 null 时不抛出异常")
    void shouldHandleNullPreviousQuestions() {
        Map<String, Object> resume = Map.of();
        Map<String, Object> jd = Map.of();

        assertDoesNotThrow(() -> {
            String message = promptBuilder.buildQuestionUserMessage(resume, jd, 1, 3, null);
            assertThat(message).doesNotContain("=== 已问过的问题 ===");
        });
    }

    @Test
    @DisplayName("问题用户消息 resumeContent 为空时不抛出异常")
    void shouldHandleEmptyResumeContent() {
        String message = promptBuilder.buildQuestionUserMessage(Map.of(), Map.of(), 1, 3, List.of());

        assertThat(message).contains("=== 候选人简历 ===");
        assertThat(message).contains("=== 目标岗位要求 ===");
    }

    // ── 回答评价 Prompt ──

    @Test
    @DisplayName("评价系统提示词非空且包含核心原则")
    void shouldBuildEvaluationSystemPrompt() {
        String prompt = promptBuilder.buildEvaluationSystemPrompt(Map.of());

        assertThat(prompt).isNotNull();
        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("面试评估专家");
        assertThat(prompt).contains("结构化评价");
        assertThat(prompt).contains("参考回答");
        assertThat(prompt).contains("真实经历");
    }

    @Test
    @DisplayName("评价用户消息包含问题和回答")
    void shouldBuildEvaluationUserMessage() {
        String message = promptBuilder.buildEvaluationUserMessage(
                "请描述你的项目经验", "我做过电商项目，使用了Spring Cloud", Map.of("title", "后端开发"), Map.of("name", "张三"));

        assertThat(message).contains("=== 面试问题 ===");
        assertThat(message).contains("请描述你的项目经验");
        assertThat(message).contains("=== 候选人回答 ===");
        assertThat(message).contains("我做过电商项目");
        assertThat(message).contains("=== 岗位要求（参考） ===");
        assertThat(message).contains("=== 候选人简历（用于生成参考回答） ===");
        assertThat(message).contains("score");
        assertThat(message).contains("strengths");
        assertThat(message).contains("weaknesses");
        assertThat(message).contains("suggestions");
        assertThat(message).contains("referenceAnswer");
    }

    @Test
    @DisplayName("评价用户消息应基于公司 Profile 生成差异化训练建议")
    void shouldIncludeCompanyProfileInEvaluationPrompt() {
        String message = promptBuilder.buildEvaluationUserMessage(
                "你如何处理需求频繁变化？",
                "我会和产品沟通后调整计划",
                Map.of("title", "后端开发"),
                Map.of("projects", List.of("订单系统")),
                companyProfile("美团")
        );

        assertThat(message).contains("=== 公司偏好 Profile（仅供回答评价参考） ===");
        assertThat(message).contains("美团", "业务落地", "指标验证");
        assertThat(message).contains("不得把 Profile 用作硬性评分或录用判断");
    }

    @Test
    @DisplayName("评价用户消息 jdContent 为 null 时不显示岗位要求段")
    void shouldHandleNullJdContentInEvaluation() {
        String message = promptBuilder.buildEvaluationUserMessage(
                "问题", "回答", null, Map.of("name", "张三"));

        assertThat(message).doesNotContain("=== 岗位要求（参考） ===");
    }

    @Test
    @DisplayName("评价用户消息 jdContent 为空时不显示岗位要求段")
    void shouldHandleEmptyJdContentInEvaluation() {
        String message = promptBuilder.buildEvaluationUserMessage(
                "问题", "回答", Map.of(), Map.of("name", "张三"));

        assertThat(message).doesNotContain("=== 岗位要求（参考） ===");
    }

    // ── 面试总结 Prompt ──

    @Test
    @DisplayName("总结系统提示词非空且包含核心原则")
    void shouldBuildSummarySystemPrompt() {
        String prompt = promptBuilder.buildSummarySystemPrompt(Map.of());

        assertThat(prompt).isNotNull();
        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("面试评估专家");
        assertThat(prompt).contains("总结报告");
        assertThat(prompt).contains("JSON Schema");
    }

    @Test
    @DisplayName("总结用户消息包含所有问答记录")
    void shouldBuildSummaryUserMessage() {
        List<Map<String, Object>> qaList = List.of(
                Map.of("questionText", "问题1", "answerText", "回答1",
                        "evaluation", Map.of("score", Map.of("clarity", 8))),
                Map.of("questionText", "问题2", "answerText", "回答2")
        );

        String message = promptBuilder.buildSummaryUserMessage(qaList);

        assertThat(message).contains("=== 面试问答记录 ===");
        assertThat(message).contains("第1题");
        assertThat(message).contains("问题1");
        assertThat(message).contains("回答1");
        assertThat(message).contains("逐题评价");
        assertThat(message).contains("clarity");
        assertThat(message).contains("第2题");
        assertThat(message).contains("问题2");
        assertThat(message).contains("overallScore");
        assertThat(message).contains("dimensionScores");
    }

    @Test
    @DisplayName("总结用户消息应包含目标公司训练方向")
    void shouldIncludeCompanyProfileInSummaryPrompt() {
        String message = promptBuilder.buildSummaryUserMessage(
                List.of(Map.of("questionText", "Q", "answerText", "A")),
                companyProfile("华为")
        );

        assertThat(message).contains("=== 公司偏好 Profile（仅供单轮面试总结参考） ===");
        assertThat(message).contains("华为", "客户价值", "工程质量");
        assertThat(message).contains("围绕该公司关注点的后续训练建议");
    }

    @Test
    @DisplayName("总结用户消息 evaluation 为 null 时不抛出异常")
    void shouldHandleNullEvaluationInSummary() {
        List<Map<String, Object>> qaList = List.of(
                Map.of("questionText", "问题1", "answerText", "回答1")
        );

        assertDoesNotThrow(() -> {
            String message = promptBuilder.buildSummaryUserMessage(qaList);
            assertThat(message).doesNotContain("逐题评价");
        });
    }

    // ── 输出 Schema ──

    @Test
    @DisplayName("问题输出 Schema 包含必需字段")
    void questionOutputSchemaContainsRequiredFields() {
        String message = promptBuilder.buildQuestionUserMessage(
                Map.of(), Map.of(), 1, 3, List.of());

        assertThat(message).contains("questionText");
        assertThat(message).contains("questionType");
        assertThat(message).contains("targetSkill");
    }

    @Test
    @DisplayName("评价输出 Schema 包含必需字段")
    void evaluationOutputSchemaContainsRequiredFields() {
        String message = promptBuilder.buildEvaluationUserMessage(
                "问题", "回答", null, Map.of("name", "张三"));

        assertThat(message).contains("\"score\"");
        assertThat(message).contains("\"strengths\"");
        assertThat(message).contains("\"weaknesses\"");
        assertThat(message).contains("\"suggestions\"");
        assertThat(message).contains("\"referenceAnswer\"");
    }

    @Test
    @DisplayName("评价 Prompt 不应使用固定样例分数锚定模型输出")
    void evaluationPromptShouldNotAnchorModelToFixedScores() {
        String message = promptBuilder.buildEvaluationUserMessage(
                "问题", "回答", null, Map.of("name", "张三"));

        assertThat(message).doesNotContain("\"clarity\": 8");
        assertThat(message).doesNotContain("\"relevance\": 7");
        assertThat(message).doesNotContain("\"depth\": 6");
        assertThat(message).doesNotContain("\"accuracy\": 8");
        assertThat(message).contains("空泛回答不得高于 6 分");
        assertThat(message).contains("四个维度不能机械相同");
    }

    @Test
    @DisplayName("总结输出 Schema 包含必需字段")
    void summaryOutputSchemaContainsRequiredFields() {
        String message = promptBuilder.buildSummaryUserMessage(
                List.of(Map.of("questionText", "Q", "answerText", "A")));

        assertThat(message).contains("overallScore");
        assertThat(message).contains("dimensionScores");
        assertThat(message).contains("technical");
        assertThat(message).contains("communication");
        assertThat(message).contains("problemSolving");
    }

    // ── PROMPT_VERSION ──

    @Test
    @DisplayName("Prompt 版本号非空")
    void shouldHaveNonEmptyPromptVersion() {
        assertThat(InterviewPromptBuilder.PROMPT_VERSION).isNotNull();
        assertThat(InterviewPromptBuilder.PROMPT_VERSION).isNotBlank();
    }

    // ── 人设与跨会话 Prompt（仅覆盖 AI 文案编排，不涉及状态机） ──

    @Test
    @DisplayName("问题系统提示词应注入面试官人设与领域要求")
    void shouldIncludePersonaInQuestionSystemPrompt() {
        String prompt = promptBuilder.buildQuestionSystemPrompt(Map.of(
                "name", "林老师", "title", "系统架构师", "style", "追问技术细节"));

        assertThat(prompt).contains("林老师", "系统架构师", "追问技术细节", "角色领域优先");
    }

    @Test
    @DisplayName("评价系统提示词应约束参考回答基于真实经历")
    void shouldIncludePersonaAndEvidenceConstraintInEvaluationPrompt() {
        String prompt = promptBuilder.buildEvaluationSystemPrompt(Map.of(
                "name", "陈老师", "title", "招聘经理", "style", "友好直接"));

        assertThat(prompt).contains("陈老师", "招聘经理", "友好直接", "不得编造虚构内容");
    }

    @Test
    @DisplayName("总结系统提示词应保留人设视角")
    void shouldIncludePersonaInSummarySystemPrompt() {
        String prompt = promptBuilder.buildSummarySystemPrompt(Map.of(
                "name", "王老师", "title", "技术负责人", "style", "严格客观"));

        assertThat(prompt).contains("王老师", "技术负责人", "严格客观", "总结视角");
    }

    @Test
    @DisplayName("评价用户消息在简历与岗位均为空时只保留必要信息")
    void shouldOmitOptionalEvaluationContextWhenEmpty() {
        String message = promptBuilder.buildEvaluationUserMessage("问题", "回答", Map.of(), Map.of());

        assertThat(message).contains("问题", "回答", "referenceAnswer");
        assertThat(message).doesNotContain("=== 岗位要求（参考） ===");
        assertThat(message).doesNotContain("=== 候选人简历（用于生成参考回答） ===");
    }

    @Test
    @DisplayName("跨会话系统提示词应声明结构化综合评估约束")
    void shouldBuildMultiSessionSystemPrompt() {
        String prompt = promptBuilder.buildMultiSessionSummarySystemPrompt();

        assertThat(prompt).contains("多场模拟面试", "共性优势", "JSON Schema");
    }

    @Test
    @DisplayName("跨会话用户消息应包含每场面试、问答和评价")
    void shouldBuildMultiSessionUserMessage() {
        List<Map<String, Object>> sessions = List.of(
                Map.of(
                        "personaName", "技术面试官",
                        "personaTitle", "架构师",
                        "qaList", List.of(Map.of(
                                "questionText", "如何保证接口幂等？",
                                "answerText", "通过唯一请求号控制",
                                "evaluation", Map.of("clarity", 8)
                        ))
                ),
                Map.of("personaName", "HR", "personaTitle", "招聘经理", "qaList", List.of())
        );

        String message = promptBuilder.buildMultiSessionSummaryUserMessage(sessions);

        assertThat(message).contains("第1场面试", "技术面试官", "如何保证接口幂等？", "唯一请求号", "clarity");
        assertThat(message).contains("第2场面试", "crossStrengths", "crossWeaknesses", "overallSummary");
    }

    @Test
    @DisplayName("跨会话复盘应融合公司 Profile 且保留边界说明")
    void shouldIncludeCompanyProfileInMultiSessionPrompt() {
        String message = promptBuilder.buildMultiSessionSummaryUserMessage(
                List.of(Map.of("personaName", "技术面试官", "personaTitle", "架构师", "qaList", List.of())),
                companyProfile("腾讯")
        );

        assertThat(message).contains("=== 公司偏好 Profile（仅供多轮面试复盘参考） ===");
        assertThat(message).contains("腾讯", "用户价值", "技术落地");
        assertThat(message).contains("不得将其写成录用概率或确定性评价");
    }

    @Test
    @DisplayName("跨会话用户消息在缺少问答列表时仍可生成")
    void shouldHandleMissingQaListInMultiSessionMessage() {
        String message = promptBuilder.buildMultiSessionSummaryUserMessage(List.of(
                Map.of("personaName", "HR", "personaTitle", "招聘经理")
        ));

        assertThat(message).contains("HR", "招聘经理", "系统性改进建议");
    }

    private Map<String, Object> companyProfile(String companyName) {
        return Map.of(
                "companyName", companyName,
                "sourceType", List.of("official", "public_interview_experience"),
                "preferenceTags", switch (companyName) {
                    case "字节跳动" -> List.of("项目深挖", "快速迭代");
                    case "美团" -> List.of("业务落地", "指标验证");
                    case "华为" -> List.of("客户价值", "工程质量");
                    case "腾讯" -> List.of("用户价值", "技术落地");
                    default -> List.of("项目真实性");
                },
                "interviewFocus", List.of("项目细节", "真实贡献"),
                "resumeAdviceRules", List.of("结合事实表达，不编造数据"),
                "confidenceLevel", "medium_high"
        );
    }
}
