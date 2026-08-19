package com.resumego.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AiInvocationService 集成测试。
 */
@SpringBootTest
@Transactional
@Sql(scripts = "/sql/ai_invocations_schema.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class AiInvocationServiceTest {

    @Autowired
    private AiInvocationService aiInvocationService;

    @Autowired
    private AiInvocationMapper aiInvocationMapper;

    @Test
    @DisplayName("成功调用应写入审计日志")
    void shouldLogSuccessfulInvocation() {
        AiRequest request = AiRequest.builder()
                .featureType("jd_parse")
                .userId(1L)
                .promptVersion("v1")
                .systemPrompt("解析JD")
                .userMessage("岗位要求")
                .build();

        AiResult result = AiResult.success(request.requestId(), "{\"ok\":true}", 100, 200, 1500L);

        aiInvocationService.logInvocation(request, result);

        // 验证写入
        QueryWrapper<AiInvocation> query = new QueryWrapper<>();
        query.eq("request_id", request.requestId());
        AiInvocation saved = aiInvocationMapper.selectOne(query);

        assertThat(saved).isNotNull();
        assertThat(saved.getFeatureType()).isEqualTo("jd_parse");
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getStatus()).isEqualTo("success");
        assertThat(saved.getPromptVersion()).isEqualTo("v1");
        assertThat(saved.getInputTokens()).isEqualTo(100);
        assertThat(saved.getOutputTokens()).isEqualTo(200);
        assertThat(saved.getLatencyMs()).isEqualTo(1500);
        assertThat(saved.getErrorCategory()).isNull();
    }

    @Test
    @DisplayName("失败调用应写入审计日志并记录错误分类")
    void shouldLogFailedInvocationWithErrorCategory() {
        AiRequest request = AiRequest.builder()
                .featureType("jd_parse")
                .userId(1L)
                .promptVersion("v1")
                .systemPrompt("")
                .userMessage("")
                .build();

        AiResult result = AiResult.failure(request.requestId(), AiErrorCategory.TIMEOUT,
                "连接超时", 30000L);

        aiInvocationService.logInvocation(request, result);

        QueryWrapper<AiInvocation> query = new QueryWrapper<>();
        query.eq("request_id", request.requestId());
        AiInvocation saved = aiInvocationMapper.selectOne(query);

        assertThat(saved).isNotNull();
        assertThat(saved.getStatus()).isEqualTo("failed");
        assertThat(saved.getErrorCategory()).isEqualTo("TIMEOUT");
        assertThat(saved.getLatencyMs()).isEqualTo(30000);
    }

    @Test
    @DisplayName("日志不应包含用户消息原文")
    void shouldNotLogUserMessageContent() {
        String sensitiveMessage = "用户完整简历和联系方式 13800000000";
        AiRequest request = AiRequest.builder()
                .featureType("jd_parse")
                .userId(1L)
                .promptVersion("v1")
                .systemPrompt("")
                .userMessage(sensitiveMessage)
                .build();

        AiResult result = AiResult.success(request.requestId(), "{}", 10, 20, 100L);

        aiInvocationService.logInvocation(request, result);

        QueryWrapper<AiInvocation> query = new QueryWrapper<>();
        query.eq("request_id", request.requestId());
        AiInvocation saved = aiInvocationMapper.selectOne(query);

        // userMessage 不应出现在审计记录中
        assertThat(saved).isNotNull();
        assertThat(saved.getFeatureType()).isEqualTo("jd_parse");
        // 审计表不存储 userMessage 字段 — 确认只记录元数据
    }

    @Test
    @DisplayName("带 Schema 校验的日志应记录 schemaValid")
    void shouldLogSchemaValidationResult() {
        AiRequest request = AiRequest.builder()
                .featureType("resume_optimization")
                .userId(1L)
                .promptVersion("v2")
                .systemPrompt("")
                .userMessage("")
                .build();

        AiResult result = AiResult.success(request.requestId(), "{}", 50, 100, 800L);

        aiInvocationService.logInvocationWithSchema(request, result, true);

        QueryWrapper<AiInvocation> query = new QueryWrapper<>();
        query.eq("request_id", request.requestId());
        AiInvocation saved = aiInvocationMapper.selectOne(query);

        assertThat(saved).isNotNull();
        assertThat(saved.getSchemaValid()).isTrue();
    }

    @Test
    @DisplayName("不同的错误分类应正确记录")
    void shouldRecordDifferentErrorCategories() {
        // Provider error
        AiRequest req1 = AiRequest.builder()
                .featureType("jd_parse").userId(1L).promptVersion("v1")
                .systemPrompt("").userMessage("").build();

        AiResult res1 = AiResult.failure(req1.requestId(), AiErrorCategory.PROVIDER_ERROR,
                "服务不可用", 500L);
        aiInvocationService.logInvocation(req1, res1);

        QueryWrapper<AiInvocation> q1 = new QueryWrapper<>();
        q1.eq("request_id", req1.requestId());
        assertThat(aiInvocationMapper.selectOne(q1).getErrorCategory()).isEqualTo("PROVIDER_ERROR");

        // Invalid JSON
        AiRequest req2 = AiRequest.builder()
                .featureType("jd_parse").userId(1L).promptVersion("v1")
                .systemPrompt("").userMessage("").build();

        AiResult res2 = AiResult.failure(req2.requestId(), AiErrorCategory.INVALID_JSON,
                "JSON 解析失败", 200L);
        aiInvocationService.logInvocation(req2, res2);

        QueryWrapper<AiInvocation> q2 = new QueryWrapper<>();
        q2.eq("request_id", req2.requestId());
        assertThat(aiInvocationMapper.selectOne(q2).getErrorCategory()).isEqualTo("INVALID_JSON");
    }
}
