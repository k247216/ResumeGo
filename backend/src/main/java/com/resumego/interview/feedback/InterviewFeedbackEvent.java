package com.resumego.interview.feedback;

/**
 * 规范化面试反馈事件：完成后由持久化总结投影生成。
 * 只引用来源对象，不反向修改简历、Pipeline、知识或 Workspace 状态。
 */
public record InterviewFeedbackEvent(
        Long sourcePlanId,
        String mode,
        String primaryIssue,
        String suggestedAction,
        String status
) {
    public static final String STATUS_PENDING = "PENDING";
}
