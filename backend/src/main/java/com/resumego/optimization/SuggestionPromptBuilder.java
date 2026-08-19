package com.resumego.optimization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.assessment.dto.ResumeAssessmentResponse;
import com.resumego.matching.dto.MatchResponse;
import com.resumego.optimization.dto.JobMatchResumeContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AI 简历优化建议 Prompt 构建器。
 * 负责构建发送给 AI 模型的系统提示词和用户消息。
 * <p>
 * Prompt 版本：v1.0
 * 更新记录：
 * - v1.0: 初始版本，支持基于 JD 缺口和能力证据的定向修改建议
 * - v1.1: AI 建议从“自动改写简历”转为“项目技术表达教练”，只输出建议与追问
 * - v1.2: 增强“薄弱点 + 训练计划”表达，服务求职能力提升闭环
 * - v1.3: 融合公司偏好 Profile，让不同目标公司的建议关注点明显区分
 */
@Component
public class SuggestionPromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(SuggestionPromptBuilder.class);
    public static final String PROMPT_VERSION = "v1.3";

    private final ObjectMapper objectMapper;

    public SuggestionPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 构建系统提示词。
     */
    public String buildSystemPrompt() {
        return """
                你是“职达”的项目技术表达教练，帮助求职者理解目标岗位希望看到的项目能力表达。
                
                核心原则：
                1. 每条修改建议必须基于用户提供的 "能力证据" 中的事实，不得编造经历、数字、技术或荣誉
                2. AI 建议不负责直接修改简历，不输出“可一键替换”的最终稿；suggestedText 表示“建议方向/参考表达策略”，由用户人工理解后修改
                3. 建议重点放在项目技术描述、工作/实习经历中的职责成果、技术选型、工程难点、指标结果、岗位相关能力和大厂偏好的表达方式
                4. 如果某项 JD 要求没有对应能力证据，将 evidenceId 设为 null，suggestedText 设为 null，并在 reason 中提出追问，要求用户补充具体项目事实
                5. 修改原因必须引用具体 JD 要求或岗位偏好，不要泛泛而谈
                6. 使用用户已有能力证据来提出表达建议，不要凭空创造经历
                7. 单次建议应覆盖多个简历模块，不要只集中在一个项目或一条经历上
                8. 每条建议都要体现“薄弱点 → 训练方向”的教练式表达，帮助用户自己修改和练习，而不是替用户完成简历
                9. 如果提供了公司偏好 Profile，只能把它作为表达风格参考，不得把经验型偏好包装成绝对事实、评分或排序依据
                10. 有公司偏好 Profile 时，每条建议应尽量体现目标公司的差异化关注点，例如业务结果、工程稳定性、用户价值、算法基础、内容场景、供应链或客户价值等，而不是输出通用建议
                
                输出格式：严格按照指定的 JSON Schema 返回，不要包含任何其他文字。
                """;
    }

    /**
     * 构建用户消息，包含简历内容、JD 要求、匹配缺口和可用证据。
     *
     * @param resumeContent 简历内容与关联证据（不可为 null）
     * @param parsedJd      JD 结构化解析结果（不可为 null）
     * @param matchGaps     匹配缺口列表（不可为 null）
     * @return 用户消息文本
     */
    public String buildUserMessage(JobMatchResumeContent resumeContent,
                                    Map<String, Object> parsedJd,
                                    List<Map<String, Object>> matchGaps) {
        return buildUserMessageWithCompanyProfile(resumeContent, parsedJd, matchGaps, Map.of());
    }

    public String buildUserMessageWithCompanyProfile(JobMatchResumeContent resumeContent,
                                                     Map<String, Object> parsedJd,
                                                     List<Map<String, Object>> matchGaps,
                                                     Map<String, Object> companyProfile) {
        if (resumeContent == null) {
            throw new IllegalArgumentException("resumeContent 不能为 null");
        }
        if (parsedJd == null) {
            throw new IllegalArgumentException("parsedJd 不能为 null");
        }
        if (matchGaps == null) {
            throw new IllegalArgumentException("matchGaps 不能为 null");
        }

        StringBuilder sb = new StringBuilder();

        sb.append("请根据以下信息，生成项目技术表达建议。注意：只给建议，不直接代写简历最终稿。\n\n");
        sb.append("=== 简历内容 ===\n");
        sb.append(toJsonString(resumeContent.content()));
        sb.append("\n\n");

        sb.append("=== 岗位要求 ===\n");
        sb.append(toJsonString(parsedJd));
        sb.append("\n\n");

        appendCompanyProfile(sb, companyProfile);

        sb.append("=== 匹配缺口 ===\n");
        sb.append(toJsonString(matchGaps));
        sb.append("\n\n");

        sb.append("=== 可用能力证据 ===\n");
        sb.append(formatEvidences(resumeContent.evidences()));
        sb.append("\n\n");

        sb.append("=== 输出格式要求 ===\n");
        sb.append(buildOutputSchema());
        sb.append("\n\n");

        sb.append("注意事项：\n");
        sb.append("- 建议数量控制在 4-6 条；如果简历中存在工作/实习经历，至少生成 1-2 条 workExperience 相关建议\n");
        sb.append("- 优先围绕项目经历、工作/实习经历、技术栈、工程难点、业务影响和岗位要求生成建议\n");
        sb.append("- 每条建议的 sectionKey 必须对应简历中的具体位置，优先使用 projects[0]、projects[1]、workExperience[0]、workExperience[1]、skills 等\n");
        sb.append("- 对 workExperience 的建议应聚焦职责是否具体、技术动作是否清晰、结果是否可验证、与目标岗位经验要求是否相关\n");
        sb.append("- 有证据时，evidenceId 填写对应证据 ID，suggestedText 写成建议方向或参考表达策略，不要写成一键替换文本\n");
        sb.append("- 无证据时，evidenceId 为 null，suggestedText 为 null，reason 中写清楚应该追问用户补充什么事实\n");
        sb.append("- reason 用“薄弱点：...；原因：...”的格式，明确指出当前表达或能力呈现的短板\n");
        sb.append("- suggestedText 用“行动建议：...；训练计划：Day 1 ... / Day 2 ... / Day 3 ...”的格式，给用户人工修改和练习的路径\n");
        sb.append("- 如果有公司偏好 Profile，reason 或 suggestedText 中至少引用一个 preferenceTags、interviewFocus 或 resumeAdviceRules 中的具体关注点；但必须写明这是表达建议参考，不是录用判断\n");
        sb.append("- confidence 取值为 high（有明确证据且匹配度高）、medium（部分证据）、low（推测性建议）\n");

        return sb.toString();
    }

    /**
     * 构建用户消息（真实模式），使用结构化匹配结果和评分数据。
     * <p>
     * 与 Mock 模式的区别：使用 MatchingPipelineService 和 ResumeAssessmentService 返回的结构化数据，
     * 替代手工从 JobMatch 表解析的 raw JSON。
     *
     * @param resumeContent      简历内容与关联证据（不可为 null）
     * @param parsedJd           JD 结构化解析结果（不可为 null）
     * @param matchResponse      匹配结果（不可为 null，来自 MatchingPipelineService）
     * @param assessmentResponse 评分结果（可为 null，来自 ResumeAssessmentService）
     * @return 用户消息文本
     */
    public String buildUserMessage(JobMatchResumeContent resumeContent,
                                    Map<String, Object> parsedJd,
                                    MatchResponse matchResponse,
                                    ResumeAssessmentResponse assessmentResponse) {
        return buildUserMessage(resumeContent, parsedJd, matchResponse, assessmentResponse, Map.of());
    }

    public String buildUserMessage(JobMatchResumeContent resumeContent,
                                    Map<String, Object> parsedJd,
                                    MatchResponse matchResponse,
                                    ResumeAssessmentResponse assessmentResponse,
                                    Map<String, Object> companyProfile) {
        if (resumeContent == null) {
            throw new IllegalArgumentException("resumeContent 不能为 null");
        }
        if (parsedJd == null) {
            throw new IllegalArgumentException("parsedJd 不能为 null");
        }
        if (matchResponse == null) {
            throw new IllegalArgumentException("matchResponse 不能为 null");
        }

        StringBuilder sb = new StringBuilder();

        sb.append("请根据以下信息，生成项目技术表达建议。注意：只给建议，不直接代写简历最终稿。\n\n");
        sb.append("=== 简历内容 ===\n");
        sb.append(toJsonString(resumeContent.content()));
        sb.append("\n\n");

        sb.append("=== 岗位要求 ===\n");
        sb.append(toJsonString(parsedJd));
        sb.append("\n\n");

        appendCompanyProfile(sb, companyProfile);

        // ── 评分数据（真实模式） ──
        if (assessmentResponse != null) {
            sb.append("=== 简历评分 ===\n");
            sb.append("总分: ").append(assessmentResponse.totalScore()).append("\n");
            sb.append("维度得分: ");
            sb.append(toJsonString(assessmentResponse.dimensionScores()));
            sb.append("\n");
            sb.append("扣分项: ");
            sb.append(toJsonString(assessmentResponse.deductions()));
            sb.append("\n\n");
        }

        // ── 匹配结果（真实模式） ──
        sb.append("=== 岗位匹配结果 ===\n");
        sb.append("综合匹配度: ").append(matchResponse.matchScore()).append("/100\n");
        sb.append("必备技能覆盖率: ").append(matchResponse.details().getRequiredCoverage()).append("%\n");
        sb.append("加分技能覆盖率: ").append(matchResponse.details().getPreferredCoverage()).append("%\n");
        sb.append("经验匹配度: ").append(matchResponse.details().getExperienceCoverage()).append("%\n");
        sb.append("已匹配项: ").append(toJsonString(matchResponse.details().getMatchedItems())).append("\n");
        sb.append("缺失项: ").append(toJsonString(matchResponse.details().getMissingItems())).append("\n");
        sb.append("维度得分: ").append(toJsonString(matchResponse.details().getDimensionScores())).append("\n\n");

        sb.append("=== 可用能力证据 ===\n");
        sb.append(formatEvidences(resumeContent.evidences()));
        sb.append("\n\n");

        sb.append("=== 输出格式要求 ===\n");
        sb.append(buildOutputSchema());
        sb.append("\n\n");

        sb.append("注意事项：\n");
        sb.append("- 建议数量控制在 4-6 条；如果简历中存在工作/实习经历，至少生成 1-2 条 workExperience 相关建议\n");
        sb.append("- 优先围绕项目经历、工作/实习经历、技术栈、工程难点、业务影响和岗位要求生成建议\n");
        sb.append("- 每条建议的 sectionKey 必须对应简历中的具体位置，优先使用 projects[0]、projects[1]、workExperience[0]、workExperience[1]、skills 等\n");
        sb.append("- 对 workExperience 的建议应聚焦职责是否具体、技术动作是否清晰、结果是否可验证、与目标岗位经验要求是否相关\n");
        sb.append("- 有证据时，evidenceId 填写对应证据 ID，suggestedText 写成建议方向或参考表达策略，不要写成一键替换文本\n");
        sb.append("- 无证据时，evidenceId 为 null，suggestedText 为 null，reason 中写清楚应该追问用户补充什么事实\n");
        sb.append("- reason 用“薄弱点：...；原因：...”的格式，明确指出当前表达或能力呈现的短板\n");
        sb.append("- suggestedText 用“行动建议：...；训练计划：Day 1 ... / Day 2 ... / Day 3 ...”的格式，给用户人工修改和练习的路径\n");
        sb.append("- 如果有公司偏好 Profile，reason 或 suggestedText 中至少引用一个 preferenceTags、interviewFocus 或 resumeAdviceRules 中的具体关注点；但必须写明这是表达建议参考，不是录用判断\n");
        sb.append("- confidence 取值为 high（有明确证据且匹配度高）、medium（部分证据）、low（推测性建议）\n");

        return sb.toString();
    }

    private void appendCompanyProfile(StringBuilder sb, Map<String, Object> companyProfile) {
        if (companyProfile == null || companyProfile.isEmpty()) {
            return;
        }
        sb.append("=== 公司偏好 Profile（仅供表达建议参考） ===\n");
        sb.append(toJsonString(companyProfile));
        sb.append("\n");
        sb.append("使用边界：该 Profile 可能来自官方资料、公开资料或经验型资料整理，只能用于调整建议的关注点和表达风格；不得作为评分、排序、录用概率或事实断言依据。\n");
        sb.append("差异化要求：请优先从 preferenceTags、interviewFocus、resumeAdviceRules 中提取 2-3 个最相关关注点，结合简历项目/经历生成具体建议。不同公司应体现不同建议重心，例如字节偏项目深挖和迭代结果，腾讯偏用户价值和技术落地，阿里偏业务背景和推动复盘，美团偏业务落地和指标验证，华为偏客户价值和工程质量。\n\n");
    }

    private String buildOutputSchema() {
        return """
                {
                  "suggestions": [
                    {
                      "sectionKey": "建议对应位置，如 projects[0]、workExperience[0] 或 skills",
                      "originalText": "当前简历中的相关原文",
                      "suggestedText": "行动建议与训练计划（不是可直接替换文本；无证据时为 null）",
                      "reason": "薄弱点诊断原因或追问问题",
                      "targetRequirement": "对应的 JD 要求",
                      "evidenceId": 1,
                      "confidence": "high"
                    }
                  ]
                }
                """;
    }

    private String formatEvidences(List<JobMatchResumeContent.CapabilityEvidenceInfo> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return "无可用证据";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < evidences.size(); i++) {
            var e = evidences.get(i);
            sb.append("证据 ").append(e.id()).append(":\n");
            sb.append("  类型: ").append(e.type()).append("\n");
            sb.append("  标题: ").append(e.title()).append("\n");
            if (e.situation() != null) {
                sb.append("  背景: ").append(e.situation()).append("\n");
            }
            sb.append("  行动: ").append(e.actionText()).append("\n");
            if (e.resultText() != null) {
                sb.append("  结果: ").append(e.resultText()).append("\n");
            }
            if (e.skillTags() != null && !e.skillTags().isEmpty()) {
                sb.append("  技能: ").append(String.join(", ", e.skillTags())).append("\n");
            }
            if (i < evidences.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

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
}
