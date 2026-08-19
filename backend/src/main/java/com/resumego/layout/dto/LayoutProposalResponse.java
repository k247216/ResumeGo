package com.resumego.layout.dto;

import java.util.List;

/**
 * AI 排版助手返回的结构化提案。
 * <p>
 * 后端仅返回提案，不直接写入简历版本。
 */
public record LayoutProposalResponse(
        String proposalId,
        String model,
        String promptVersion,
        List<LayoutProposalChangeDTO> changes,
        String templateKey,
        List<String> hiddenSectionIds,
        List<String> warnings
) {
}
