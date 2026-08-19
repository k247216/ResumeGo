package com.resumego.optimization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.ai.AiClient;
import com.resumego.ai.AiClientSelector;
import com.resumego.ai.AiErrorCategory;
import com.resumego.ai.AiInvocationService;
import com.resumego.ai.AiRequest;
import com.resumego.ai.AiResult;
import com.resumego.ai.validate.AiOutputValidator;
import com.resumego.common.CurrentUser;
import com.resumego.optimization.dto.SuggestionFollowUpRequest;
import com.resumego.optimization.dto.SuggestionFollowUpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 建议追问服务。
 *
 * <p>只生成最终建议，不修改简历、不更新建议状态、不创建版本。</p>
 */
@Service
public class SuggestionFollowUpService {

    private static final Logger log = LoggerFactory.getLogger(SuggestionFollowUpService.class);

    public static final String PROMPT_VERSION = "project-coach-followup-v1";
    private static final String FEATURE_TYPE = "suggestion_followup";

    private final OptimizationSuggestionMapper suggestionMapper;
    private final AiClient aiClient;
    private final AiInvocationService aiInvocationService;
    private final AiOutputValidator outputValidator;
    private final ObjectMapper objectMapper;

    public SuggestionFollowUpService(
            OptimizationSuggestionMapper suggestionMapper,
            AiClientSelector aiClientSelector,
            AiInvocationService aiInvocationService,
            AiOutputValidator outputValidator,
            ObjectMapper objectMapper
    ) {
        this.suggestionMapper = suggestionMapper;
        this.aiClient = aiClientSelector.getClient();
        this.aiInvocationService = aiInvocationService;
        this.outputValidator = outputValidator;
        this.objectMapper = objectMapper;
    }

    public SuggestionFollowUpResponse generateFinalAdvice(Long suggestionId, SuggestionFollowUpRequest request) {
        if (suggestionId == null || suggestionId <= 0) {
            throw new IllegalArgumentException("suggestionId 必须为正整数");
        }
        String supplement = request == null ? null : request.userSupplement();
        if (supplement == null || supplement.trim().length() < 8) {
            throw new IllegalArgumentException("补充事实过短");
        }
        if (supplement.length() > 1200) {
            throw new IllegalArgumentException("补充事实不能超过 1200 字");
        }

        OptimizationSuggestion suggestion = suggestionMapper.selectById(suggestionId);
        if (suggestion == null) {
            throw new IllegalArgumentException("建议不存在");
        }

        String requestId = UUID.randomUUID().toString();
        AiRequest aiRequest = AiRequest.builder()
                .requestId(requestId)
                .featureType(FEATURE_TYPE)
                .userId(CurrentUser.DEMO_USER_ID)
                .promptVersion(PROMPT_VERSION)
                .systemPrompt(systemPrompt())
                .userMessage(userMessage(suggestion, supplement.trim()))
                .build();

        long start = System.currentTimeMillis();
        AiResult aiResult;
        try {
            aiResult = aiClient.invoke(aiRequest);
        } catch (Exception exception) {
            log.error("AI 建议追问调用异常: requestId={}", requestId, exception);
            aiResult = AiResult.failure(requestId, AiErrorCategory.PROVIDER_ERROR,
                    "AI 服务异常，请稍后重试", System.currentTimeMillis() - start);
        }

        boolean schemaValid = false;
        SuggestionFollowUpResponse response = null;
        if (aiResult.success() && aiResult.content() != null) {
            try {
                String json = outputValidator.extractJson(aiResult.content());
                var validation = outputValidator.validateJson(json);
                if (validation.isValid()) {
                    response = parseResponse(json);
                    schemaValid = true;
                }
            } catch (Exception exception) {
                log.warn("AI 建议追问输出校验失败: requestId={}", requestId, exception);
            }
        }

        aiInvocationService.logInvocationWithSchema(aiRequest, aiResult, schemaValid);

        if (response != null) {
            return response;
        }
        return fallbackAdvice(suggestion);
    }

    private String systemPrompt() {
        return """
                你是“职达”的项目技术表达教练。
                用户已经根据追问补充了项目事实，你需要给出最终建议，帮助用户自己修改简历。

                规则：
                1. 只输出建议，不直接代写完整简历段落，不创建可一键替换文本。
                2. 不得编造补充事实中没有出现的技术、指标、公司、奖项、规模或结果。
                3. 建议聚焦项目技术描述：技术动作、工程难点、岗位相关能力、结果呈现、需要核实的信息。
                4. 如果用户补充的信息仍不足，nextSteps 中继续给出应该补充或核实的问题。
                5. 不要原样复述用户补充内容，不要输出联系方式、账号、访问令牌等敏感信息。
                6. 只返回 JSON，不要返回 Markdown。

                JSON Schema:
                {
                  "finalAdvice": "面向用户的最终建议",
                  "nextSteps": ["下一步人工修改或核实事项"]
                }
                """;
    }

    private String userMessage(OptimizationSuggestion suggestion, String supplement) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sectionKey", suggestion.getSectionKey());
            payload.put("originalText", nullToEmpty(suggestion.getOriginalText()));
            payload.put("initialAdvice", nullToEmpty(suggestion.getSuggestedText()));
            payload.put("initialReason", nullToEmpty(suggestion.getReasonText()));
            payload.put("targetRequirement", nullToEmpty(suggestion.getTargetRequirement()));
            payload.put("userSupplement", supplement);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("构建追问 Prompt 失败", exception);
        }
    }

    private SuggestionFollowUpResponse parseResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        String finalAdvice = text(root, "finalAdvice");
        if (finalAdvice == null || finalAdvice.isBlank()) {
            throw new IllegalArgumentException("finalAdvice 为空");
        }
        if (finalAdvice.length() > 1200) {
            finalAdvice = finalAdvice.substring(0, 1200);
        }
        List<String> nextSteps = parseStringArray(root.get("nextSteps")).stream()
                .limit(5)
                .toList();
        return new SuggestionFollowUpResponse(finalAdvice, nextSteps, PROMPT_VERSION);
    }

    private SuggestionFollowUpResponse fallbackAdvice(OptimizationSuggestion suggestion) {
        String finalAdvice = "你补充的事实可以用于强化「" + nullToEmpty(suggestion.getTargetRequirement())
                + "」相关表达。建议手动整理为：项目背景 → 你的技术动作 → 遇到的工程难点 → 使用的技术栈 → 可验证结果。"
                + " 如仍缺少真实指标，可采用过程型表达，避免夸大结果。";
        return new SuggestionFollowUpResponse(finalAdvice, List.of(
                "核实补充内容中涉及的技术和指标是否真实可证明",
                "回到对应项目模块，将事实压缩成 1-2 条项目亮点",
                "不要写入没有证据支撑的规模、性能或获奖描述"
        ), PROMPT_VERSION);
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private List<String> parseStringArray(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            if (item != null && item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText());
            }
        });
        return values;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
