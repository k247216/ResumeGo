package com.resumego.layout.dto;

/**
 * 单条可审查的排版/措辞修改。
 */
public record LayoutProposalChangeDTO(
        String id,
        String sectionId,
        String fieldKey,
        String label,
        String before,
        String after,
        String reason,
        String riskLevel
) {
}
