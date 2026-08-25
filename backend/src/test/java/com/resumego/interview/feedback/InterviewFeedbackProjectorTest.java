package com.resumego.interview.feedback;

import com.resumego.interview.dto.MultiSessionSummaryResponse;
import com.resumego.interview.entity.InterviewPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 反馈投影测试：PENDING 状态、核心问题与建议映射、诚实空值、不修改来源。 */
@DisplayName("InterviewFeedbackProjector 投影")
class InterviewFeedbackProjectorTest {

    private final InterviewFeedbackProjector projector = new InterviewFeedbackProjector();

    private MultiSessionSummaryResponse summary() {
        return new MultiSessionSummaryResponse(
                "整体稳定",
                82,
                List.of("基础扎实"),
                List.of("项目结果表达偏弱", "系统设计深度不足"),
                List.of("补充项目量化指标", "复盘系统设计"),
                List.of()
        );
    }

    private InterviewPlan plan(String mode) {
        InterviewPlan plan = new InterviewPlan();
        plan.setId(100L);
        plan.setUserId(1L);
        plan.setMode(mode);
        return plan;
    }

    @Test
    @DisplayName("完成总结投影为 PENDING 事件：核心问题取首个薄弱点，建议取首条建议")
    void projectsPendingEventWithPrimaryIssueAndAction() {
        InterviewFeedbackEvent event = projector.project(plan("KNOWLEDGE_TRAINING"), summary());

        assertThat(event.sourcePlanId()).isEqualTo(100L);
        assertThat(event.mode()).isEqualTo("KNOWLEDGE_TRAINING");
        assertThat(event.primaryIssue()).isEqualTo("项目结果表达偏弱");
        assertThat(event.suggestedAction()).isEqualTo("补充项目量化指标");
        assertThat(event.status()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("总结缺薄弱点/建议时如实为 null，不编造")
    void projectsHonestNullsWhenSummaryPartsMissing() {
        MultiSessionSummaryResponse sparse = new MultiSessionSummaryResponse(
                "总结", 70, List.of(), List.of(), List.of(), List.of());

        InterviewFeedbackEvent event = projector.project(plan("ROLE_BASED"), sparse);

        assertThat(event.primaryIssue()).isNull();
        assertThat(event.suggestedAction()).isNull();
        assertThat(event.status()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("缺计划或总结直接拒绝")
    void rejectsMissingInputs() {
        assertThatThrownBy(() -> projector.project(null, summary()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> projector.project(plan("ROLE_BASED"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
