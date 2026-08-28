package com.resumego.interview.service;


import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 面试 AI Prompt 构建器。
 * <p>
 * 负责构建问题生成、回答评价和面试总结三个场景的 Prompt。
 * 所有 Prompt 内容仅用于 AI 生成内容，不参与状态机决策。
 * <p>
 * Prompt 版本：v1.3
 */
@Component
public class InterviewPromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(InterviewPromptBuilder.class);
    public static final String PROMPT_VERSION = "v1.3";

    private final ObjectMapper objectMapper;

    public InterviewPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ── 问题生成 ──

    /**
     * 构建问题生成的系统提示词。
     */
    public String buildQuestionSystemPrompt(Map<String, String> personaContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的模拟面试官。\n\n");

        if (personaContext != null && !personaContext.isEmpty()) {
            String personaName = personaContext.getOrDefault("name", "");
            String personaTitle = personaContext.getOrDefault("title", "");
            String personaStyle = personaContext.getOrDefault("style", "");

            sb.append("=== 你的面试官角色（必须严格遵循，这是问题生成的首要依据） ===\n");
            sb.append("姓名：").append(personaName).append("\n");
            sb.append("职位：").append(personaContext.get("title")).append("\n");
            sb.append("风格：").append(personaContext.get("style")).append("\n\n");

            sb.append("角色行为要求：\n");
            sb.append("1. 提问视角：你必须从「").append(personaTitle).append("」的视角出发，提出该角色最关心的那类问题。\n");
            sb.append("   例如：HR 总监应侧重软技能、文化匹配、沟通协作；架构师应侧重系统设计、技术选型、架构决策。\n");
            sb.append("2. 提问风格：你必须严格遵循风格描述「").append(personaStyle).append("」。\n");
            sb.append("   这意味着你的提问方式、追问深度、语气态度都应与该风格完全一致。\n");
            sb.append("   例如：\"高压面试官\"应语气犀利、连续追问、制造压力场景；\"友好面试官\"应语气鼓励、给予肯定、营造轻松氛围。\n");
            sb.append("3. 问题差异化：不同角色的面试官必须问出不同类型的问题，绝不能与其他面试官角色雷同。\n");
            sb.append("   如果候选人简历中有技术项目，架构师应深挖技术细节，而 HR 应关注团队协作和项目推动过程。\n");
            sb.append("4. 角色领域优先：你的专业领域问题是必须出现的，简历只是参考材料。\n");
            sb.append("   如果简历中缺乏你专业领域的内容，你仍然应该提出该领域的问题（如通用场景题、假设题），而不是跳过。\n");
            sb.append("   例如：简历未提算法，但你是算法专家，你仍应问算法思路、复杂度分析、优化思维等通用算法问题。\n");
            sb.append("5. 开场问候：如果你是第一位面试官，首次提问时可以用「").append(personaName).append("」的身份做简短开场，但不要过长。\n\n");
        }

        sb.append("""
                核心原则：
                1. 【角色优先】问题的主题和方向由你的面试官角色决定，简历和岗位要求是辅助参考而非硬性约束
                2. 【领域覆盖】如果你擅长的领域（如算法、系统设计、软技能）在简历中未体现，你仍应提出该领域问题
                3. 【简历关联】当简历中有相关经历时，优先结合简历内容提问；无相关经历时，使用通用场景题或假设题
                4. 问题应有递进性，后续问题可以基于前面的回答深入追问
                5. 问题应具体、可回答，避免过于宽泛
                6. 不要重复已经问过的问题
                
                输出格式：严格按照指定的 JSON Schema 返回，不要包含任何其他文字。
                """);
        return sb.toString();
    }

    /**
     * 构建问题生成的用户消息。
     *
     * @param resumeContent     简历内容
     * @param jdContent         岗位要求
     * @param questionIndex     当前题号（从 1 开始）
     * @param totalQuestions    总题数
     * @param previousQuestions 已问过的问题列表（可为空）
     */
    public String buildQuestionUserMessage(Map<String, Object> resumeContent,
                                            Map<String, Object> jdContent,
                                            int questionIndex,
                                            int totalQuestions,
                                            List<Map<String, String>> previousQuestions) {
        return buildQuestionUserMessage(resumeContent, jdContent, questionIndex, totalQuestions, previousQuestions, Map.of());
    }

    public String buildQuestionUserMessage(Map<String, Object> resumeContent,
                                            Map<String, Object> jdContent,
                                            int questionIndex,
                                            int totalQuestions,
                                            List<Map<String, String>> previousQuestions,
                                            Map<String, Object> companyProfile) {
        StringBuilder sb = new StringBuilder();
        sb.append("请生成第 ").append(questionIndex).append("/").append(totalQuestions).append(" 个面试问题。\n\n");

        sb.append("=== 候选人简历 ===\n");
        sb.append(toJsonString(resumeContent));
        sb.append("\n\n");

        sb.append("=== 目标岗位要求 ===\n");
        sb.append(toJsonString(jdContent));
        sb.append("\n\n");

        appendCompanyProfile(sb, companyProfile, "面试问题生成");

        if (previousQuestions != null && !previousQuestions.isEmpty()) {
            sb.append("=== 已问过的问题 ===\n");
            for (int i = 0; i < previousQuestions.size(); i++) {
                Map<String, String> q = previousQuestions.get(i);
                sb.append("第").append(i + 1).append("题: ").append(q.get("questionText")).append("\n");
                sb.append("  类型: ").append(q.get("questionType")).append("\n");
            }
            sb.append("\n");
        }

        sb.append("=== 输出格式要求 ===\n");
        sb.append(buildQuestionOutputSchema());
        sb.append("\n\n");

        sb.append("注意事项：\n");
        sb.append("- questionType 可选值：behavioral（行为面试）、technical（技术技能）、situational（情景题）、background（背景了解）、other（其他）\n");
        sb.append("- targetSkill 填写该问题考察的核心技能名称\n");
        sb.append("- 第 1 题建议从 background 类型开始，了解候选人背景\n");
        sb.append("- 如果提供了公司偏好 Profile，问题应体现该公司的差异化关注点；例如项目深挖、用户价值、业务落地、算法基础、工程质量或客户价值，但不得声称这是该公司的官方录用标准。\n");
        sb.append("- 记住：你的角色决定了问题方向，简历只是素材。简历中没有的内容不代表不能问，用通用场景题或假设题代替即可。\n");

        return sb.toString();
    }

    // ── 回答评价 ──

    /**
     * 构建回答评价的系统提示词。
     */
    public String buildEvaluationSystemPrompt(Map<String, String> personaContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的面试评估专家，对候选人的面试回答进行结构化评价。\n\n");

        if (personaContext != null && !personaContext.isEmpty()) {
            String personaName = personaContext.getOrDefault("name", "");
            String personaStyle = personaContext.getOrDefault("style", "");

            sb.append("=== 你的面试官角色（必须严格遵循） ===\n");
            sb.append("姓名：").append(personaName).append("\n");
            sb.append("职位：").append(personaContext.get("title")).append("\n");
            sb.append("风格：").append(personaStyle).append("\n\n");

            sb.append("角色行为要求：\n");
            sb.append("1. 评价视角：你必须从「").append(personaName).append("」的视角进行评价，关注该角色最看重的维度。\n");
            sb.append("   例如：架构师应侧重技术深度和系统设计合理性；HR 应侧重沟通表达和文化匹配度。\n");
            sb.append("2. 评价语气：你的评价语气必须与风格「").append(personaStyle).append("」完全一致。\n");
            sb.append("   例如：\"高压面试官\"的评价应严格犀利、直指要害；\"友好面试官\"的评价应鼓励为主、温和指出不足。\n\n");
        }

        sb.append("""
                核心原则：
                1. 评价必须客观、具体，基于候选人的实际回答内容
                2. 优点和不足都需要有具体依据
                3. 改进建议应具有可操作性
                4. 评分维度关注：表达清晰度、内容相关性、思考深度、技术准确性
                5. 参考回答必须严格基于候选人简历中的真实经历，不得编造虚构内容
                
                输出格式：严格按照指定的 JSON Schema 返回，不要包含任何其他文字。
                """);
        return sb.toString();
    }

    /**
     * 构建回答评价的用户消息。
     *
     * @param questionText 问题文本
     * @param answerText   用户回答
     * @param jdContent    岗位要求（可为 null）
     * @param resumeContent 简历内容（用于生成参考回答）
     */
    public String buildEvaluationUserMessage(String questionText, String answerText,
                                              Map<String, Object> jdContent,
                                              Map<String, Object> resumeContent) {
        return buildEvaluationUserMessage(questionText, answerText, jdContent, resumeContent, Map.of());
    }

    public String buildEvaluationUserMessage(String questionText, String answerText,
                                              Map<String, Object> jdContent,
                                              Map<String, Object> resumeContent,
                                              Map<String, Object> companyProfile) {
        StringBuilder sb = new StringBuilder();
        sb.append("请对以下面试回答进行评价，并生成一个基于候选人简历真实经历的参考回答。\n\n");

        sb.append("=== 面试问题 ===\n");
        sb.append(questionText);
        sb.append("\n\n");

        sb.append("=== 候选人回答 ===\n");
        sb.append(answerText);
        sb.append("\n\n");

        if (jdContent != null && !jdContent.isEmpty()) {
            sb.append("=== 岗位要求（参考） ===\n");
            sb.append(toJsonString(jdContent));
            sb.append("\n\n");
        }

        if (resumeContent != null && !resumeContent.isEmpty()) {
            sb.append("=== 候选人简历（用于生成参考回答） ===\n");
            sb.append(toJsonString(resumeContent));
            sb.append("\n\n");
        } else {
            sb.append("=== 上下文限制 ===\n");
            sb.append("本次练习没有可用的候选人简历。referenceAnswer 必须返回空字符串，不得编造用户经历；反馈只基于题目、回答和已提供的资料。\n\n");
        }

        appendCompanyProfile(sb, companyProfile, "回答评价");

        sb.append("=== 输出格式要求 ===\n");
        sb.append(buildEvaluationOutputSchema());
        sb.append("\n\n");

        sb.append("注意事项：\n");
        sb.append("- 每个评分维度取 1-10 分\n");
        sb.append("- 分数必须依据候选人回答内容拉开差异，不能因为输出示例而给固定分\n");
        sb.append("- 空泛回答不得高于 6 分；包含具体场景、个人动作、技术细节和可核实结果的回答才可给 8-10 分\n");
        sb.append("- 四个维度不能机械相同，应分别反映表达、岗位相关性、技术深度和准确性\n");
        sb.append("- strengths 和 weaknesses 至少各列出 1 条\n");
        sb.append("- suggestions 提供具体的改进建议\n");
        sb.append("- 如果提供了公司偏好 Profile，weaknesses 或 suggestions 中应体现与目标公司关注点相关的训练建议；但不得把 Profile 用作硬性评分或录用判断。\n");
        sb.append("- referenceAnswer 必须基于候选人简历中的真实经历，不得编造虚构内容\n");
        sb.append("- referenceAnswer 应展示理想的回答结构：背景 → 行动 → 结果 → 复盘\n");

        return sb.toString();
    }

    // ── 面试总结 ──

    /**
     * 构建面试总结的系统提示词。
     */
    public String buildSummarySystemPrompt(Map<String, String> personaContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位资深的面试评估专家，根据候选人在模拟面试中的全部表现生成总结报告。\n\n");

        if (personaContext != null && !personaContext.isEmpty()) {
            String personaName = personaContext.getOrDefault("name", "");
            String personaStyle = personaContext.getOrDefault("style", "");

            sb.append("=== 你的面试官角色（必须严格遵循） ===\n");
            sb.append("姓名：").append(personaName).append("\n");
            sb.append("职位：").append(personaContext.get("title")).append("\n");
            sb.append("风格：").append(personaStyle).append("\n\n");

            sb.append("角色行为要求：\n");
            sb.append("1. 总结视角：你必须从「").append(personaName).append("」的视角进行总结，重点评价从你的专业角度来看哪些方面需要提升。\n");
            sb.append("2. 总结语气：你的总结必须与风格「").append(personaStyle).append("」保持一致。\n");
            sb.append("   例如：高压面试官的总结直接指出问题不绕弯子；友好面试官的总结多鼓励、多肯定、建设性地提出改进方向。\n\n");
        }

        sb.append("""
                核心原则：
                1. 总结应全面反映候选人的整体表现
                2. 指出候选人的核心优势和待提升领域
                3. 提供针对性的改进建议
                4. 评价应客观、建设性
                
                输出格式：严格按照指定的 JSON Schema 返回，不要包含任何其他文字。
                """);
        return sb.toString();
    }

    /**
     * 构建面试总结的用户消息。
     *
     * @param qaList 问题-回答-评价列表
     */
    public String buildSummaryUserMessage(List<Map<String, Object>> qaList) {
        return buildSummaryUserMessage(qaList, Map.of());
    }

    public String buildSummaryUserMessage(List<Map<String, Object>> qaList,
                                          Map<String, Object> companyProfile) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下面试问答记录，生成面试总结报告。\n\n");

        sb.append("=== 面试问答记录 ===\n");
        for (int i = 0; i < qaList.size(); i++) {
            Map<String, Object> qa = qaList.get(i);
            sb.append("--- 第").append(i + 1).append("题 ---\n");
            sb.append("问题: ").append(qa.get("questionText")).append("\n");
            sb.append("回答: ").append(qa.get("answerText")).append("\n");
            Object eval = qa.get("evaluation");
            if (eval != null) {
                sb.append("逐题评价: ").append(toJsonString(eval)).append("\n");
            }
            sb.append("\n");
        }

        appendCompanyProfile(sb, companyProfile, "单轮面试总结");

        sb.append("=== 输出格式要求 ===\n");
        sb.append(buildSummaryOutputSchema());
        sb.append("\n\n");

        sb.append("注意事项：\n");
        sb.append("- overallScore 取 1-100 分，综合反映整体面试表现\n");
        sb.append("- dimensionScores 包含 technical、communication、problemSolving 三个维度，各取 1-10 分\n");
        sb.append("- strengths 列出 3-5 条核心优势\n");
        sb.append("- weaknesses 列出 2-3 条待提升领域\n");
        sb.append("- suggestions 提供 3-5 条具体改进建议\n");
        sb.append("- 如果提供了公司偏好 Profile，suggestions 至少包含 1-2 条围绕该公司关注点的后续训练建议，例如项目深挖、业务价值、工程稳定性、算法基础、内容场景或客户价值。\n");

        return sb.toString();
    }

    // ── 跨会话总结 ──

    /**
     * 构建跨会话总结的系统提示词。
     */
    public String buildMultiSessionSummarySystemPrompt() {
        return """
                你是一位资深的职业发展顾问，根据候选人在多场模拟面试中的全部表现生成综合评估报告。
                
                核心原则：
                1. 跨越不同面试官和场景，识别候选人的共性优势和短板
                2. 关注不同面试风格下的表现差异
                3. 提供系统性的改进建议
                4. 评价应客观、建设性，有数据支撑
                
                输出格式：严格按照指定的 JSON Schema 返回，不要包含任何其他文字。
                """;
    }

    /**
     * 构建跨会话总结的用户消息。
     *
     * @param sessionSummaries 每个会话的摘要信息（包含面试官、问答记录等）
     */
    public String buildMultiSessionSummaryUserMessage(List<Map<String, Object>> sessionSummaries) {
        return buildMultiSessionSummaryUserMessage(sessionSummaries, Map.of());
    }

    public String buildMultiSessionSummaryUserMessage(List<Map<String, Object>> sessionSummaries,
                                                      Map<String, Object> companyProfile) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下多场模拟面试的问答记录，生成综合评估报告。\n\n");

        sb.append("=== 各场面试记录 ===\n");
        for (int i = 0; i < sessionSummaries.size(); i++) {
            Map<String, Object> session = sessionSummaries.get(i);
            sb.append("--- 第").append(i + 1).append("场面试 ---\n");
            sb.append("面试官：").append(session.get("personaName")).append("（")
                    .append(session.get("personaTitle")).append("）\n");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> qaList = (List<Map<String, Object>>) session.get("qaList");
            if (qaList != null) {
                sb.append("共 ").append(qaList.size()).append(" 题：\n");
                for (int j = 0; j < qaList.size(); j++) {
                    Map<String, Object> qa = qaList.get(j);
                    sb.append("  Q").append(j + 1).append(": ").append(qa.get("questionText")).append("\n");
                    sb.append("  A").append(j + 1).append(": ").append(qa.get("answerText")).append("\n");
                    Object eval = qa.get("evaluation");
                    if (eval != null) {
                        sb.append("  评价: ").append(toJsonString(eval)).append("\n");
                    }
                }
            }
            sb.append("\n");
        }

        appendCompanyProfile(sb, companyProfile, "多轮面试复盘");

        sb.append("=== 输出格式要求 ===\n");
        sb.append(buildMultiSessionSummaryOutputSchema());
        sb.append("\n\n");

        sb.append("注意事项：\n");
        sb.append("- overallScore 取 1-100 分，综合反映多场面试的整体表现\n");
        sb.append("- overallSummary 提供一段综合性文字总结（200-400字）\n");
        sb.append("- crossStrengths 列出跨会话的共性优势 3-5 条\n");
        sb.append("- crossWeaknesses 列出跨会话的共性不足 2-4 条\n");
        sb.append("- suggestions 提供系统性改进建议 3-5 条\n");
        sb.append("- 如果提供了公司偏好 Profile，overallSummary 和 suggestions 应体现目标公司差异化关注点，但不得将其写成录用概率或确定性评价。\n");

        return sb.toString();
    }

    // ── 输出 Schema ──

    private String buildQuestionOutputSchema() {
        return """
                {
                  "questionText": "面试问题文本",
                  "questionType": "behavioral",
                  "targetSkill": "考察的核心技能"
                }
                """;
    }

    private void appendCompanyProfile(StringBuilder sb, Map<String, Object> companyProfile, String scene) {
        if (companyProfile == null || companyProfile.isEmpty()) {
            return;
        }
        sb.append("=== 公司偏好 Profile（仅供").append(scene).append("参考） ===\n");
        sb.append(toJsonString(companyProfile));
        sb.append("\n");
        sb.append("使用边界：该 Profile 只用于让问题、评价和训练建议更贴近目标公司的公开/经验型关注点；不得用于控制面试状态、题号、结束条件、分数规则、录用概率或确定性事实断言。\n");
        sb.append("差异化要求：优先结合 preferenceTags、interviewFocus、resumeAdviceRules 中的具体词，生成更有公司辨识度的问题和建议。\n\n");
    }

    private String buildEvaluationOutputSchema() {
        return """
                {
                  "score": {
                    "clarity": "1-10 的整数，表达清晰度",
                    "relevance": "1-10 的整数，与问题和岗位要求的相关性",
                    "depth": "1-10 的整数，技术深度和思考深度",
                    "structure": "1-10 的整数，回答结构是否完整、层次是否清楚",
                    "evidence": "1-10 的整数，是否有具体可核实的事实、动作和结果"
                  },
                  "strengths": ["优点1", "优点2"],
                  "weaknesses": ["不足1"],
                  "suggestions": ["改进建议1"],
                  "referenceAnswer": "基于候选人简历真实经历的示范回答"
                }
                """;
    }

    private String buildSummaryOutputSchema() {
        return """
                {
                  "overallScore": 75,
                  "dimensionScores": {
                    "technical": 8,
                    "communication": 7,
                    "problemSolving": 6
                  },
                  "strengths": ["核心优势1", "核心优势2"],
                  "weaknesses": ["待提升1", "待提升2"],
                  "suggestions": ["改进建议1", "改进建议2"]
                }
                """;
    }

    private String buildMultiSessionSummaryOutputSchema() {
        return """
                {
                  "overallScore": 75,
                  "overallSummary": "200-400字的综合性文字总结",
                  "crossStrengths": ["共性优势1", "共性优势2"],
                  "crossWeaknesses": ["共性不足1", "共性不足2"],
                  "suggestions": ["系统性改进建议1", "系统性改进建议2"]
                }
                """;
    }

    // ── 辅助方法 ──

    private String toJsonString(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON 序列化失败，无法构建 Prompt", e);
            throw new IllegalStateException("JSON 序列化失败，无法构建 Prompt", e);
        }
    }

    // ═══════ 知识训练出题 ═══════

    /** 知识训练出题系统提示词：结论必须引用文档片段，找不到依据必须明确说明。 */
    public String buildKnowledgeQuestionSystemPrompt() {
        return """
你是一位严谨的技术教练，基于用户提供的知识资料出练习题。

硬性规则：
1. 只能围绕资料中的真实内容出题；每个问题必须标注来源文档编号和支撑片段原文。
2. 如果资料不足以支撑某个方向的问题，不要编造，直接跳过该方向。
3. 输出必须是 JSON 对象：{"questions":[{"questionText":"...","questionType":"基础|深入|场景","sourceDocumentId":<数字>,"quote":"支撑片段原文（不超过100字）"}]}
4. 不要输出 JSON 以外的任何文字。
""";
    }

    /** 知识训练出题用户消息：资料内容截断拼接，避免超长。 */
    public String buildKnowledgeQuestionUserPrompt(java.util.Map<Long, String> documentContents, int count, String difficulty) {
        return buildKnowledgeQuestionUserPrompt(documentContents, count, difficulty, null);
    }

    /** 知识训练出题用户消息：提问风格作为训练节奏提示，不能改变资料来源边界。 */
    public String buildKnowledgeQuestionUserPrompt(java.util.Map<Long, String> documentContents,
                                                   int count,
                                                   String difficulty,
                                                   String questionStyle) {
        StringBuilder sb = new StringBuilder();
        sb.append("请基于以下知识资料出 ").append(count).append(" 道题");
        if (difficulty != null && !difficulty.isBlank()) {
            sb.append("，难度方向：").append(difficulty);
        }
        if (questionStyle != null && !questionStyle.isBlank()) {
            sb.append("，提问风格：").append(questionStyle);
        }
        sb.append("\n");
        for (java.util.Map.Entry<Long, String> entry : documentContents.entrySet()) {
            sb.append("\n=== 资料文档 #").append(entry.getKey()).append(" ===\n");
            String content = entry.getValue();
            int limit = Math.min(content.length(), 6000);
            sb.append(content, 0, limit);
            if (content.length() > limit) sb.append("…（已截断）");
            sb.append('\n');
        }
        return sb.toString();
    }
}
