package com.resumego.optimization.dto;

import java.util.List;

/**
 * 生成 AI 优化建议响应。
 */
public record GenerateSuggestionsResponse(
        List<OptimizationSuggestionDTO> suggestions
) {
}
