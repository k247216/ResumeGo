package com.resumego.interview.dto;

import com.resumego.interview.InterviewMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateInterviewPlanRequestQuestionCountTest {

    @Test
    void acceptsModeSpecificQuestionCountLimits() {
        assertThatCode(() -> role(5).toContext()).doesNotThrowAnyException();
        assertThatCode(() -> role(15).toContext()).doesNotThrowAnyException();
        assertThatCode(() -> knowledge(1).toContext()).doesNotThrowAnyException();
        assertThatCode(() -> knowledge(20).toContext()).doesNotThrowAnyException();
        assertThatCode(() -> experience(30).toContext()).doesNotThrowAnyException();
    }

    @Test
    void rejectsQuestionCountOutsideModeLimit() {
        assertThatThrownBy(() -> role(4).toContext())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("岗位模拟题目数量应为 5-15");
        assertThatThrownBy(() -> role(16).toContext())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("岗位模拟题目数量应为 5-15");
        assertThatThrownBy(() -> knowledge(0).toContext())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("知识训练题目数量应为 1-20");
        assertThatThrownBy(() -> knowledge(21).toContext())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("知识训练题目数量应为 1-20");
        assertThatThrownBy(() -> experience(0).toContext())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("面经演练题目数量应为 1-30");
        assertThatThrownBy(() -> experience(31).toContext())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("面经演练题目数量应为 1-30");
    }

    @Test
    void experienceReviewModeIsKeptInTheTypedContext() {
        CreateInterviewPlanRequest request = new CreateInterviewPlanRequest(
                InterviewMode.EXPERIENCE_SIMULATION, null, null, null, null, null,
                5L, "高压", "PER_QUESTION", 10, null, List.of("项目"), null);

        assertThat(((com.resumego.interview.context.InterviewStartContext.ExperienceSimulation) request.toContext())
                .reviewMode()).isEqualTo("PER_QUESTION");
    }

    @Test
    void experienceQuestionOrderIsKeptInTheTypedContext() {
        CreateInterviewPlanRequest request = new CreateInterviewPlanRequest(
                InterviewMode.EXPERIENCE_SIMULATION, null, null, null, null, null,
                5L, "高压", "PER_QUESTION", List.of(2, 0, 1), 3, null, List.of(), null);

        assertThat(((com.resumego.interview.context.InterviewStartContext.ExperienceSimulation) request.toContext())
                .questionOrder()).containsExactly(2, 0, 1);
    }

    private CreateInterviewPlanRequest role(int count) {
        return new CreateInterviewPlanRequest(InterviewMode.ROLE_BASED, 1L, 2L,
                null, null, null, null, count, List.of(3L), List.of(), null);
    }

    private CreateInterviewPlanRequest knowledge(int count) {
        return new CreateInterviewPlanRequest(InterviewMode.KNOWLEDGE_TRAINING, null, null,
                List.of(4L), "基础", null, null, count, null, List.of(), null);
    }

    private CreateInterviewPlanRequest experience(int count) {
        return new CreateInterviewPlanRequest(InterviewMode.EXPERIENCE_SIMULATION, null, null,
                null, null, 5L, "适中", count, null, List.of(), null);
    }
}
