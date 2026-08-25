package com.resumego.interview.feedback;

import com.resumego.interview.dto.MultiSessionSummaryResponse;
import com.resumego.interview.entity.InterviewPlan;
import org.springframework.stereotype.Component;

/**
 * 最小反馈投影：只映射持久化总结中的最核心问题（首个薄弱点）与建议动作（首条建议）。
 * 不计算全局排名，不自动接受行动；源资料归档后仍可从持久化总结回放投影。
 */
@Component
public class InterviewFeedbackProjector {

    public InterviewFeedbackEvent project(InterviewPlan plan, MultiSessionSummaryResponse summary) {
        if (plan == null || summary == null) {
            throw new IllegalArgumentException("反馈投影需要计划与总结");
        }
        String primaryIssue = summary.crossWeaknesses() != null && !summary.crossWeaknesses().isEmpty()
                ? summary.crossWeaknesses().get(0)
                : null;
        String suggestedAction = summary.suggestions() != null && !summary.suggestions().isEmpty()
                ? summary.suggestions().get(0)
                : null;
        return new InterviewFeedbackEvent(
                plan.getId(),
                plan.getMode(),
                primaryIssue,
                suggestedAction,
                InterviewFeedbackEvent.STATUS_PENDING
        );
    }
}
