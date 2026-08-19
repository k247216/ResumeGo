package com.resumego.layout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * AI 排版助手 Prompt 构造器。
 * <p>
 * 只提供排版相关文本，不提供电话、邮箱、微信等敏感信息。
 */
public class LayoutProposalPromptBuilder {

    public static final String PROMPT_VERSION = "resume-layout-v1";

    private final ObjectMapper objectMapper;

    public LayoutProposalPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildSystemPrompt() {
        return """
                你是“职达”的简历排版与措辞优化助手。
                你的任务是基于用户提供的简历草稿文本和目标岗位摘要，生成可审查的 JSON 修改提案。

                规则：
                1. 只能压缩、润色、调整表达密度，不得新增用户没有提供的经历、数字、技能、公司、奖项或结果。
                2. 只能修改输入 editableFields 中提供的 fieldKey，不能修改分数、岗位匹配排序、面试状态、权限或版本状态。
                3. before 必须来自对应 fieldKey 的原文，after 必须保持事实一致且更适合简历排版。
                4. 尽量覆盖不同 sectionId：如果 editableFields 覆盖了多个模块，请优先每个有内容的模块给 1 条建议，不要只改 summary。
                5. 如果提供了 targetJob，请围绕岗位要求重排表达重点：优先突出原文中已经出现的岗位相关技术、职责、项目影响和协作动作。
                6. 修改要有实际价值，避免只替换一两个无关词；每条建议应说明具体解决了“冗长、重点不突出、岗位相关性弱、表达不简历化”中的哪类问题。
                7. 对技能项、技术栈、亮点列表等字段，after 可以输出用“、”分隔的一行文本，但不得新增原文没有的技能、数字或经历。
                8. templateKey 默认返回 null；只有在文本改写仍不足以解决一页排版密度时，才建议切换模板。
                9. hiddenSectionIds 只能包含输入 emptySectionIds 中确实已添加且为空的模块；不要隐藏未添加模块。
                10. 如果证据不足，不要补写事实；可以在 warnings 中说明。
                11. 只返回 JSON，不要返回 Markdown，不要解释。

                JSON Schema:
                {
                  "changes": [
                    {
                      "fieldKey": "projects.0.description",
                      "before": "原文",
                      "after": "优化后文本",
                      "reason": "优化原因",
                      "riskLevel": "low"
                    }
                  ],
                  "templateKey": "compact 或 null",
                  "hiddenSectionIds": ["custom"],
                  "warnings": ["说明"]
                }
                """;
    }

    public String buildUserMessage(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("构造排版助手 Prompt 失败", e);
        }
    }

    public Map<String, Object> buildPayload(
            String templateKey,
            String goal,
            Map<String, Object> targetJob,
            List<String> activeSectionIds,
            List<Map<String, Object>> editableFields,
            List<String> emptySectionIds
    ) {
        return Map.of(
                "templateKey", templateKey == null ? "" : templateKey,
                "goal", goal == null ? "compress_to_one_page" : goal,
                "targetJob", targetJob == null ? Map.of() : targetJob,
                "activeSectionIds", activeSectionIds == null ? List.of() : activeSectionIds,
                "editableFields", editableFields,
                "emptySectionIds", emptySectionIds
        );
    }
}
