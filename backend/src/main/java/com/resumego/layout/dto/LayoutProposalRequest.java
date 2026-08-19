package com.resumego.layout.dto;

import java.util.Map;

/**
 * 生成简历排版与措辞优化提案的请求。
 *
 * @param resumeVersionId         当前基础版本，可为空白草稿时为空
 * @param draftContent            前端当前草稿内容
 * @param targetJobDescriptionId  当前绑定岗位，可为空
 * @param targetJob              当前绑定岗位摘要，可为空，不包含用户隐私
 * @param templateKey             当前模板
 * @param goal                    优化目标，例如 compress_to_one_page
 */
public record LayoutProposalRequest(
        Long resumeVersionId,
        Map<String, Object> draftContent,
        Long targetJobDescriptionId,
        Map<String, Object> targetJob,
        String templateKey,
        String goal
) {
}
