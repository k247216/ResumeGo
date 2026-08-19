package com.resumego.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MockAiClient 单元测试。
 */
class MockAiClientTest {

    private MockAiClient mockAiClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockAiClient = new MockAiClient(objectMapper);
    }

    @AfterEach
    void clearErrorMode() {
        System.clearProperty("mock.ai.error.mode");
        Thread.interrupted();
    }

    @Test
    @DisplayName("调用 jd_parse 功能返回有效的 mock 响应")
    void shouldReturnValidJdParseMockResponse() {
        AiRequest request = AiRequest.builder()
                .featureType("jd_parse")
                .userId(1L)
                .promptVersion("v1")
                .systemPrompt("解析以下JD")
                .userMessage("岗位要求 Java Spring Boot")
                .build();

        AiResult result = mockAiClient.invoke(request);

        assertThat(result.success()).isTrue();
        assertThat(result.content()).contains("requiredSkills");
        assertThat(result.content()).contains("Java");
        assertThat(result.latencyMs()).isPositive();
        assertThat(result.requestId()).isEqualTo(request.requestId());
        assertThat(result.errorCategory()).isNull();
    }

    @Test
    @DisplayName("调用 resume_optimization 功能返回有效的 mock 响应")
    void shouldReturnValidResumeOptimizationMockResponse() {
        AiRequest request = AiRequest.builder()
                .featureType("resume_optimization")
                .userId(1L)
                .promptVersion("v1")
                .systemPrompt("优化建议")
                .userMessage("简历内容")
                .build();

        AiResult result = mockAiClient.invoke(request);

        assertThat(result.success()).isTrue();
        assertThat(result.content()).contains("suggestions");
    }

    @Test
    @DisplayName("调用 interview_evaluation 时应根据回答质量返回有区分度的 mock 分数")
    void shouldReturnVariedInterviewEvaluationScoresByAnswerQuality() throws Exception {
        AiRequest strongAnswerRequest = AiRequest.builder()
                .featureType("interview_evaluation")
                .userId(1L)
                .promptVersion("v1")
                .systemPrompt("评价面试回答")
                .userMessage("""
                        === 面试问题 ===
                        请介绍你做过的订单系统优化。

                        === 候选人回答 ===
                        我负责订单查询链路优化，先通过慢查询日志定位到缺少联合索引的问题，
                        然后重写 SQL 并增加 Redis 缓存热点订单，最终接口 P95 延迟从 800ms 降到 120ms，
                        支撑了促销期间约 5 万 QPS 的流量。
                        """)
                .build();
        AiRequest weakAnswerRequest = AiRequest.builder()
                .featureType("interview_evaluation")
                .userId(1L)
                .promptVersion("v1")
                .systemPrompt("评价面试回答")
                .userMessage("""
                        === 面试问题 ===
                        请介绍你做过的订单系统优化。

                        === 候选人回答 ===
                        我参与过这个项目，主要就是做了一些后端开发，感觉还可以，学到了很多。
                        """)
                .build();

        int strongTotal = interviewScoreTotal(mockAiClient.invoke(strongAnswerRequest).content());
        int weakTotal = interviewScoreTotal(mockAiClient.invoke(weakAnswerRequest).content());

        assertThat(strongTotal).isGreaterThan(weakTotal);
        assertThat(strongTotal - weakTotal).isGreaterThanOrEqualTo(6);
    }

    @Test
    @DisplayName("调用 interview_evaluation 时中等回答不应稳定返回 8/7/6/8")
    void shouldAvoidFixedInterviewEvaluationScorePattern() throws Exception {
        AiRequest firstRequest = AiRequest.builder()
                .featureType("interview_evaluation")
                .userId(1L)
                .promptVersion("v1")
                .systemPrompt("评价面试回答")
                .userMessage("""
                        === 面试问题 ===
                        请介绍你做过的项目难点。

                        === 候选人回答 ===
                        我负责后台接口开发，主要使用 Spring Boot 和 MySQL，过程中排查过接口响应慢的问题，
                        通过分析日志和调整查询条件完成了优化，最后让页面查询速度有明显提升。
                        """)
                .build();
        AiRequest secondRequest = AiRequest.builder()
                .featureType("interview_evaluation")
                .userId(1L)
                .promptVersion("v1")
                .systemPrompt("评价面试回答")
                .userMessage("""
                        === 面试问题 ===
                        请介绍你做过的项目难点。

                        === 候选人回答 ===
                        我在课程项目里做过消息通知模块，使用 Redis 做临时状态缓存，
                        也处理过重复发送和异常重试的问题，但当时没有记录很完整的量化数据。
                        """)
                .build();

        var firstScore = interviewScoreVector(mockAiClient.invoke(firstRequest).content());
        var secondScore = interviewScoreVector(mockAiClient.invoke(secondRequest).content());

        assertThat(firstScore).isNotEqualTo(List.of(8, 7, 6, 8));
        assertThat(secondScore).isNotEqualTo(List.of(8, 7, 6, 8));
        assertThat(firstScore).isNotEqualTo(secondScore);
    }

    @Test
    @DisplayName("调用未知功能类型返回通用 mock 响应")
    void shouldReturnGenericMockForUnknownFeature() {
        AiRequest request = AiRequest.builder()
                .featureType("unknown_feature")
                .userId(1L)
                .promptVersion("v1")
                .systemPrompt("test")
                .userMessage("test")
                .build();

        AiResult result = mockAiClient.invoke(request);

        assertThat(result.success()).isTrue();
        assertThat(result.content().toLowerCase()).contains("mock");
    }

    @Test
    @DisplayName("Mock 调用应有合理延迟")
    void shouldHaveReasonableLatency() {
        AiRequest request = AiRequest.builder()
                .featureType("jd_parse")
                .userId(1L)
                .promptVersion("v1")
                .systemPrompt("")
                .userMessage("")
                .build();

        AiResult result = mockAiClient.invoke(request);

        // Mock 延迟应在 50-250ms 之间
        assertThat(result.latencyMs()).isGreaterThanOrEqualTo(50);
        assertThat(result.latencyMs()).isLessThanOrEqualTo(500);
    }

    @Test
    @DisplayName("多次调用的延迟应有变化")
    void shouldHaveVariedLatency() {
        AiRequest request = AiRequest.builder()
                .featureType("jd_parse")
                .userId(1L)
                .promptVersion("v1")
                .systemPrompt("")
                .userMessage("")
                .build();

        long latency1 = mockAiClient.invoke(request).latencyMs();
        long latency2 = mockAiClient.invoke(request).latencyMs();

        // 多次调用的延迟不应完全相同（随机范围内）
        assertThat(Math.abs(latency1 - latency2)).isLessThanOrEqualTo(200);
    }

    @Test
    @DisplayName("排版 Mock 应按字段生成安全压缩提案并标出空模块")
    void shouldBuildLayoutResponseFromPayload() throws Exception {
        String longText = "负责 Java 服务端模块开发，完成接口设计、异常处理、测试回归和部署联调，保障核心功能稳定交付并持续优化用户体验。".repeat(4);
        String payload = objectMapper.writeValueAsString(java.util.Map.of(
                "editableFields", List.of(
                        java.util.Map.of("fieldKey", "summary", "sectionId", "summary", "text", longText),
                        java.util.Map.of("fieldKey", "projects.0.description", "sectionId", "projects", "text", longText),
                        java.util.Map.of("fieldKey", "projects.0.highlights", "sectionId", "projects", "text", longText)
                ),
                "emptySectionIds", List.of("certifications", "languages", "github")
        ));

        AiResult result = mockAiClient.invoke(request("resume_layout", payload));
        var root = objectMapper.readTree(result.content());

        assertThat(root.path("changes")).hasSize(2);
        assertThat(root.path("changes").get(0).path("riskLevel").asText()).isEqualTo("low");
        assertThat(root.path("hiddenSectionIds")).hasSize(2);
        assertThat(root.path("warnings").get(0).asText()).contains("Mock 模式");
    }

    @Test
    @DisplayName("其余受支持 AI 场景应返回结构化响应")
    void shouldReturnStructuredResponsesForOtherFeatures() throws Exception {
        var followUp = objectMapper.readTree(mockAiClient.invoke(request("suggestion_followup", "补充项目事实")).content());
        var question = objectMapper.readTree(mockAiClient.invoke(request("interview_question", "简历")).content());
        var summary = objectMapper.readTree(mockAiClient.invoke(request("interview_summary", "问答记录")).content());

        assertThat(followUp.path("finalAdvice").asText()).contains("不要加入");
        assertThat(followUp.path("nextSteps")).hasSize(3);
        assertThat(question.path("questionType").asText()).isEqualTo("technical");
        assertThat(summary.path("dimensionScores").path("technical").asInt()).isBetween(1, 10);
    }

    @Test
    @DisplayName("错误模式应支持非法 JSON、虚构证据模拟和提供方失败")
    void shouldSupportConfiguredErrorModes() throws Exception {
        System.setProperty("mock.ai.error.mode", "invalid_json");
        assertThat(mockAiClient.invoke(request("jd_parse", "JD")).content()).contains("parse error");

        System.setProperty("mock.ai.error.mode", "fake_evidence");
        var fake = objectMapper.readTree(mockAiClient.invoke(request("resume_optimization", "简历")).content());
        assertThat(fake.path("suggestions").get(0).path("suggestedText").asText())
                .contains("核实", "不要写入简历");

        System.setProperty("mock.ai.error.mode", "provider_failure");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mockAiClient.invoke(request("jd_parse", "JD")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("provider_failure");
    }

    @Test
    @DisplayName("线程中断时 Mock 应返回可识别的失败结果")
    void shouldReturnFailureWhenInterrupted() {
        Thread.currentThread().interrupt();

        AiResult result = mockAiClient.invoke(request("jd_parse", "JD"));

        assertThat(result.success()).isFalse();
        assertThat(result.errorCategory()).isEqualTo(AiErrorCategory.UNKNOWN);
        assertThat(result.errorMessage()).contains("中断");
    }

    private AiRequest request(String featureType, String userMessage) {
        return AiRequest.builder()
                .featureType(featureType)
                .userId(1L)
                .promptVersion("v1")
                .systemPrompt("test")
                .userMessage(userMessage)
                .build();
    }

    private int interviewScoreTotal(String content) throws Exception {
        return interviewScoreVector(content).stream().mapToInt(Integer::intValue).sum();
    }

    private List<Integer> interviewScoreVector(String content) throws Exception {
        var score = objectMapper.readTree(content).path("score");
        return List.of(
                score.path("clarity").asInt(),
                score.path("relevance").asInt(),
                score.path("depth").asInt(),
                score.path("accuracy").asInt());
    }
}
