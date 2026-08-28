package com.resumego.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock AI 实现，用于无 API Key 时的本地模拟。
 * <p>
 * 不发起任何网络请求，返回预制的结构化响应。
 * 延迟 50-200ms 模拟网络耗时。
 * <p>
 * 支持通过 System Property {@code mock.ai.error.mode} 切换错误模式：
 * <ul>
 *   <li>{@code invalid_json} — 返回非 JSON 文本，模拟 AI 输出格式错误</li>
 *   <li>{@code fake_evidence} — 返回虚构内容，触发编造事实检测</li>
 *   <li>{@code provider_failure} — 抛出异常，模拟 AI 服务调用失败</li>
 * </ul>
 * 不设置或设置为其他值时，使用正常模式。
 */
public class MockAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(MockAiClient.class);

    private static final String ERROR_MODE_KEY = "mock.ai.error.mode";

    private final ObjectMapper objectMapper;

    public MockAiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AiResult invoke(AiRequest request) {
        long start = System.currentTimeMillis();

        // 模拟网络延迟 50-200ms
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(50, 200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return AiResult.failure(request.requestId(), AiErrorCategory.UNKNOWN,
                    "Mock 调用被中断", System.currentTimeMillis() - start);
        }

        // ── 错误模式：模拟 AI 服务调用失败（抛异常） ──
        String errorMode = System.getProperty(ERROR_MODE_KEY);
        if ("provider_failure".equals(errorMode)) {
            long latencyMs = System.currentTimeMillis() - start;
            log.warn("Mock 错误模式: provider_failure, requestId={}", request.requestId());
            throw new RuntimeException("Mock AI 服务调用失败（provider_failure 模式）");
        }

        long latencyMs = System.currentTimeMillis() - start;

        String mockContent = buildMockResponse(request.featureType(), request.userMessage());
        log.debug("Mock AI 调用完成: featureType={}, requestId={}, latencyMs={}",
                request.featureType(), request.requestId(), latencyMs);

        return AiResult.success(request.requestId(), mockContent, 10, 20, latencyMs);
    }

    /**
     * 根据功能类型返回不同的 mock 响应。
     */
    private String buildMockResponse(String featureType, String userMessage) {
        try {
            String errorMode = System.getProperty(ERROR_MODE_KEY);

            // ── 错误模式：返回非 JSON 文本 ──
            if ("invalid_json".equals(errorMode)) {
                return "AI response failed: parse error at line 1, unexpected token";
            }

            return switch (featureType) {
                case "jd_parse" -> objectMapper.writeValueAsString(Map.of(
                        "requiredSkills", new String[]{"Java", "Spring Boot", "MySQL"},
                        "preferredSkills", new String[]{"Docker"},
                        "responsibilities", new String[]{"负责后端业务接口开发", "参与数据库表设计"},
                        "experienceRequirements", new String[]{"有课程项目或实习经验"},
                        "educationRequirements", new String[]{"本科及以上学历"}
                ));
                case "resume_optimize", "resume_optimization" -> {
                        Map<String, Object> suggestion1 = new HashMap<>();
                        suggestion1.put("sectionKey", "projects[0]");

                        if ("fake_evidence".equals(errorMode)) {
                            // ── 错误模式：虚构经历（编造检测） ──
                            suggestion1.put("originalText", "参与项目开发");
                            suggestion1.put("suggestedText", "建议先核实该项目是否真的涉及 Kubernetes、团队规模、流量指标和获奖事实；没有事实支撑时不要写入简历。");
                            suggestion1.put("reason", "JD要求大型系统经验，但当前项目描述没有足够事实支撑，需要避免夸大。");
                            suggestion1.put("targetRequirement", "有大型系统架构经验");
                        } else {
                            suggestion1.put("originalText", "负责项目开发工作");
                            suggestion1.put("suggestedText", "建议把项目技术描述拆成：你的具体职责、使用的 Spring Boot/MySQL 等技术、遇到的工程难点、已有证据中的性能或稳定性结果；如果没有指标，先补充真实数据再写。");
                            suggestion1.put("reason", "JD关注高并发和工程实践，当前项目表达过于笼统，需要突出技术动作和可验证结果。");
                            suggestion1.put("targetRequirement", "有高并发系统设计经验");
                        }
                        suggestion1.put("evidenceId", 1);
                        suggestion1.put("confidence", "high");

                        Map<String, Object> suggestion2 = new HashMap<>();
                        suggestion2.put("sectionKey", "skills");
                        suggestion2.put("originalText", "熟悉Java");
                        suggestion2.put("suggestedText", null);
                        suggestion2.put("reason", "追问：这个项目是否实际使用过 Docker？如果使用过，请补充镜像构建、部署环境、Compose/K8s、遇到的问题和你的处理动作；如果没有，不建议写入。");
                        suggestion2.put("targetRequirement", "熟悉Docker容器化部署");
                        suggestion2.put("evidenceId", null);
                        suggestion2.put("confidence", "low");

                        Map<String, Object> suggestion3 = new HashMap<>();
                        suggestion3.put("sectionKey", "workExperience[0]");
                        suggestion3.put("originalText", "负责后端接口开发");
                        suggestion3.put("suggestedText", null);
                        suggestion3.put("reason", "追问：这段工作或实习经历中，你具体负责了哪些接口、模块或排查任务？是否有代码质量、性能、稳定性、协作流程方面的真实结果可以补充？");
                        suggestion3.put("targetRequirement", "具备真实工程开发或实习经验");
                        suggestion3.put("evidenceId", null);
                        suggestion3.put("confidence", "low");

                        yield objectMapper.writeValueAsString(Map.of(
                                "suggestions", new Object[]{suggestion1, suggestion2, suggestion3}
                        ));
                    }
                case "resume_layout" -> buildMockLayoutResponse(userMessage);
                case "suggestion_followup" -> objectMapper.writeValueAsString(Map.of(
                        "finalAdvice", "可以把你补充的事实整理为：先说明项目场景和目标，再写你负责的技术动作，最后补充真实可核实的难点、结果或指标。不要加入补充事实中没有出现的大厂偏好词、团队规模或性能数据。",
                        "nextSteps", new String[]{
                                "核实补充信息中的技术、指标和结果是否真实可证明",
                                "回到对应项目模块，手动整理为 1-2 条项目技术亮点",
                                "如果仍缺少结果数据，可以改写为过程型表达，避免夸大"
                        }
                ));
                case "interview_question" -> objectMapper.writeValueAsString(Map.of(
                        "questionText", "请介绍一下你在简历中提到的Spring Boot项目中遇到的最具挑战性的技术问题，以及你是如何解决它的？",
                        "questionType", "technical",
                        "targetSkill", "Spring Boot 问题排查与解决"
                ));
                case "interview_evaluation" -> buildMockInterviewEvaluationResponse(userMessage);
                case "interview_summary" -> objectMapper.writeValueAsString(Map.of(
                        "overallScore", 75,
                        "dimensionScores", Map.of(
                                "technical", 8,
                                "communication", 7,
                                "problemSolving", 6
                        ),
                        "strengths", new String[]{"问题分析能力较强", "表达清晰有条理"},
                        "weaknesses", new String[]{"可以进一步提升量化思维", "需要加强深度思考"},
                        "suggestions", new String[]{"多练习用STAR法则回答问题", "阅读更多技术深度文章"}
                ));
                default -> objectMapper.writeValueAsString(Map.of(
                        "message", "Mock response for feature: " + featureType
                ));
            };
        } catch (JsonProcessingException e) {
            return "{\"message\": \"mock fallback\"}";
        }
    }

    private String buildMockLayoutResponse(String userMessage) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(userMessage == null ? "{}" : userMessage);
        JsonNode editableFields = root.path("editableFields");
        List<Map<String, Object>> changes = new ArrayList<>();

        if (editableFields.isArray() && editableFields.size() > 0) {
            List<String> usedSections = new ArrayList<>();
            for (JsonNode field : editableFields) {
                if (changes.size() >= 8) break;
                String fieldKey = field.path("fieldKey").asText("");
                String sectionId = field.path("sectionId").asText("");
                if (!sectionId.isBlank() && usedSections.contains(sectionId)) {
                    continue;
                }
                String text = field.path("text").asText("").trim();
                String before = text.length() > 180 ? text.substring(0, 180).trim() : text;
                String after = compactMockLayoutText(before);
                if (!fieldKey.isBlank() && !before.isBlank() && after.length() < before.length()) {
                    changes.add(Map.of(
                            "fieldKey", fieldKey,
                            "before", before,
                            "after", after,
                            "reason", "Mock 模式：按模块压缩表达，保留原有事实并提升排版密度",
                            "riskLevel", "low"
                    ));
                    if (!sectionId.isBlank()) {
                        usedSections.add(sectionId);
                    }
                }
            }
        }

        List<String> hiddenSectionIds = new ArrayList<>();
        JsonNode emptySections = root.path("emptySectionIds");
        if (emptySections.isArray()) {
            for (JsonNode section : emptySections) {
                if (hiddenSectionIds.size() >= 2) break;
                String sectionId = section.asText("");
                if (!sectionId.isBlank()) {
                    hiddenSectionIds.add(sectionId);
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("changes", changes);
        response.put("templateKey", null);
        response.put("hiddenSectionIds", hiddenSectionIds);
        response.put("warnings", List.of("Mock 模式下基于当前草稿生成排版提案"));
        return objectMapper.writeValueAsString(response);
    }

    private String compactMockLayoutText(String text) {
        String normalized = text
                .replaceAll("\\s+", " ")
                .replace("，并且", "，")
                .replace("进行了", "完成")
                .replace("负责参与", "参与")
                .trim();
        if (normalized.length() <= 130) {
            return normalized;
        }
        return normalized.substring(0, 130).trim() + "…";
    }

    private String buildMockInterviewEvaluationResponse(String userMessage) throws JsonProcessingException {
        String answer = extractSection(userMessage, "=== 候选人回答 ===");
        Map<String, Integer> score = buildInterviewScore(answer);
        int total = score.values().stream().mapToInt(Integer::intValue).sum();

        String[] strengths = total >= 30
                ? new String[]{"回答包含具体技术动作、问题定位过程和结果指标，可信度较高"}
                : total >= 24
                ? new String[]{"回答能覆盖问题方向，但关键细节和结果表达还不够充分"}
                : new String[]{"回答能回应问题，但表达较空泛，缺少可验证的技术细节"};
        String[] weaknesses = total >= 30
                ? new String[]{"可以继续补充取舍过程、失败尝试和复盘沉淀，让回答更有层次"}
                : total >= 24
                ? new String[]{"需要补充更明确的技术动作、业务结果或量化指标"}
                : new String[]{"缺少具体场景、个人职责、技术动作和结果指标，难以体现真实能力"};
        String[] suggestions = total >= 30
                ? new String[]{"建议按 背景-动作-结果-复盘 的结构压缩表达，突出最关键的一次技术决策"}
                : total >= 24
                ? new String[]{"建议补充你具体负责的模块、使用的技术、遇到的问题和可核实结果"}
                : new String[]{"建议先补齐真实项目事实，再用 STAR 法组织回答，避免只说“参与、学习、了解”"};

        return objectMapper.writeValueAsString(Map.of(
                "score", score,
                "strengths", strengths,
                "weaknesses", weaknesses,
                "suggestions", suggestions,
                "referenceAnswer", "参考回答应基于你的真实经历组织为：项目背景 → 你负责的模块 → 具体技术动作 → 可核实结果 → 复盘收获。Mock 模式不会替你编造不存在的公司、指标或荣誉。"
        ));
    }

    private Map<String, Integer> buildInterviewScore(String answer) {
        String normalized = answer == null ? "" : answer.replaceAll("\\s+", " ").trim();
        int length = normalized.length();
        int metricSignals = countMatches(normalized, "\\d+|%|ms|qps|QPS|万|次|倍|条");
        int actionSignals = countContains(normalized, List.of(
                "负责", "定位", "分析", "优化", "设计", "实现", "排查", "重构", "引入", "通过", "使用", "基于", "最终"
        ));
        int techSignals = countContains(normalized, List.of(
                "Java", "Spring", "Spring Boot", "MySQL", "Redis", "Docker", "Kafka", "SQL", "索引", "缓存", "接口", "数据库", "并发", "延迟", "链路"
        ));
        int structureSignals = countContains(normalized, List.of(
                "首先", "然后", "最后", "因为", "所以", "同时", "结果", "复盘", "背景", "难点", "方案"
        ));
        int ownershipSignals = countContains(normalized, List.of(
                "我负责", "我设计", "我实现", "我定位", "我排查", "我优化", "我推动", "我的职责"
        ));
        int vagueSignals = countContains(normalized, List.of(
                "还可以", "学到了很多", "参与过", "了解", "一些", "感觉", "差不多", "比较简单"
        ));

        int evidenceCap = 10;
        if (length < 40) {
            evidenceCap = Math.min(evidenceCap, 6);
        }
        if (techSignals == 0) {
            evidenceCap = Math.min(evidenceCap, 7);
        }
        if (actionSignals < 2) {
            evidenceCap = Math.min(evidenceCap, 7);
        }
        if (metricSignals == 0) {
            evidenceCap = Math.min(evidenceCap, 8);
        }
        if (vagueSignals >= 2) {
            evidenceCap = Math.min(evidenceCap, 6);
        }

        int clarity = 3
                + Math.min(3, length / 70)
                + Math.min(2, structureSignals / 2)
                + Math.min(1, ownershipSignals)
                - Math.min(2, vagueSignals / 2);
        int relevance = 3
                + Math.min(3, techSignals / 2)
                + Math.min(2, actionSignals / 3)
                + Math.min(1, ownershipSignals)
                - Math.min(2, vagueSignals / 2);
        int depth = 2
                + Math.min(3, actionSignals / 2)
                + Math.min(2, techSignals / 3)
                + Math.min(1, metricSignals / 2)
                + Math.min(1, structureSignals / 3)
                - Math.min(2, vagueSignals / 2);
        int accuracy = 3
                + Math.min(3, metricSignals)
                + Math.min(2, techSignals / 3)
                + Math.min(1, ownershipSignals)
                - Math.min(2, vagueSignals / 2);

        int clarityScore = clampScore(clarity + dimensionOffset(normalized, "clarity"), evidenceCap);
        int relevanceScore = clampScore(relevance + dimensionOffset(normalized, "relevance"), evidenceCap);
        int depthScore = clampScore(depth + dimensionOffset(normalized, "depth") + (metricSignals > 0 ? 1 : 0), evidenceCap);
        int accuracyScore = clampScore(accuracy + dimensionOffset(normalized, "accuracy"), evidenceCap);
        int structureScore = clampScore(
                3 + Math.min(4, structureSignals / 2) + Math.min(2, ownershipSignals / 2)
                        - Math.min(2, vagueSignals / 2), evidenceCap);
        int evidenceScore = clampScore(
                3 + Math.min(4, metricSignals) + Math.min(2, ownershipSignals)
                        + Math.min(1, actionSignals / 3) - Math.min(2, vagueSignals / 2), evidenceCap);

        if (clarityScore == 8 && relevanceScore == 7 && depthScore == 6 && accuracyScore == 8) {
            depthScore = Math.max(1, depthScore - 1);
        }

        return Map.of(
                "clarity", clarityScore,
                "relevance", relevanceScore,
                "depth", depthScore,
                "structure", structureScore,
                "evidence", evidenceScore,
                "accuracy", evidenceScore
        );
    }

    private String extractSection(String text, String marker) {
        if (text == null || marker == null) {
            return "";
        }
        int start = text.indexOf(marker);
        if (start < 0) {
            return text;
        }
        String section = text.substring(start + marker.length());
        int nextMarker = section.indexOf("\n===");
        if (nextMarker >= 0) {
            section = section.substring(0, nextMarker);
        }
        return section.trim();
    }

    private int countMatches(String text, String regex) {
        return (int) java.util.regex.Pattern.compile(regex)
                .matcher(text == null ? "" : text)
                .results()
                .count();
    }

    private int countContains(String text, List<String> tokens) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String token : tokens) {
            if (text.contains(token)) {
                count++;
            }
        }
        return count;
    }

    private int dimensionOffset(String text, String dimension) {
        int bucket = Math.floorMod((text + "#" + dimension).hashCode(), 5);
        return switch (bucket) {
            case 0 -> -1;
            case 4 -> 1;
            default -> 0;
        };
    }

    private int clampScore(int score, int maxScore) {
        return Math.max(1, Math.min(maxScore, score));
    }
}
