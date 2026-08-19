package com.resumego.optimization.dto;

import java.util.List;

/**
 * AI 建议追问后的最终建议响应。
 *
 * @param finalAdvice   最终建议，供用户人工理解和修改简历
 * @param nextSteps     建议用户下一步手动补充或核实的事项
 * @param promptVersion Prompt 版本
 */
public record SuggestionFollowUpResponse(
        String finalAdvice,
        List<String> nextSteps,
        String promptVersion
) {
}
