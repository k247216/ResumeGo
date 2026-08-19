package com.resumego.interview.service;

import com.resumego.interview.InterviewAction;
import com.resumego.interview.InterviewState;
import com.resumego.interview.InterviewTransitionTable;
import com.resumego.interview.entity.InterviewSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * InterviewStateMachine 单元测试。
 * 覆盖所有合法转换、非法转换、终态判断和分支逻辑。
 */
@DisplayName("InterviewStateMachine 单元测试")
class InterviewStateMachineTest {

    private InterviewStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new InterviewStateMachine();
    }

    // ── 合法转换 ──

    @Nested
    @DisplayName("合法状态转换")
    class ValidTransitions {

        @Test
        @DisplayName("READY + START → ASKING")
        void readyToAsking() {
            InterviewSession session = createSession(InterviewState.READY);
            stateMachine.transition(session, InterviewAction.START);
            assertThat(session.getStatus()).isEqualTo(InterviewState.ASKING.name());
            assertThat(session.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("ASKING + QUESTION_READY → WAITING_ANSWER")
        void askingToWaitingAnswer() {
            InterviewSession session = createSession(InterviewState.ASKING);
            stateMachine.transition(session, InterviewAction.QUESTION_READY);
            assertThat(session.getStatus()).isEqualTo(InterviewState.WAITING_ANSWER.name());
        }

        @Test
        @DisplayName("WAITING_ANSWER + ANSWER_SUBMITTED → EVALUATING")
        void waitingAnswerToEvaluating() {
            InterviewSession session = createSession(InterviewState.WAITING_ANSWER);
            stateMachine.transition(session, InterviewAction.ANSWER_SUBMITTED);
            assertThat(session.getStatus()).isEqualTo(InterviewState.EVALUATING.name());
        }

        @Test
        @DisplayName("EVALUATING + GO_NEXT → ASKING")
        void evaluatingToAsking() {
            InterviewSession session = createSession(InterviewState.EVALUATING);
            stateMachine.transition(session, InterviewAction.GO_NEXT);
            assertThat(session.getStatus()).isEqualTo(InterviewState.ASKING.name());
        }

        @Test
        @DisplayName("EVALUATING + GO_SUMMARIZE → SUMMARIZING")
        void evaluatingToSummarizing() {
            InterviewSession session = createSession(InterviewState.EVALUATING);
            stateMachine.transition(session, InterviewAction.GO_SUMMARIZE);
            assertThat(session.getStatus()).isEqualTo(InterviewState.SUMMARIZING.name());
        }

        @Test
        @DisplayName("SUMMARIZING + SUMMARY_READY → COMPLETED")
        void summarizingToCompleted() {
            InterviewSession session = createSession(InterviewState.SUMMARIZING);
            stateMachine.transition(session, InterviewAction.SUMMARY_READY);
            assertThat(session.getStatus()).isEqualTo(InterviewState.COMPLETED.name());
            assertThat(session.getCompletedAt()).isNotNull();
        }
    }

    // ── 非法转换 ──

    @Nested
    @DisplayName("非法状态转换")
    class InvalidTransitions {

        @Test
        @DisplayName("READY + START 之外的任何动作应抛出异常")
        void readyOnlyAcceptsStart() {
            InterviewSession session = createSession(InterviewState.READY);
            assertThatThrownBy(() -> stateMachine.transition(session, InterviewAction.ANSWER_SUBMITTED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("非法的状态转换");
        }

        @Test
        @DisplayName("WAITING_ANSWER + START 应抛出异常")
        void waitingAnswerRejectsStart() {
            InterviewSession session = createSession(InterviewState.WAITING_ANSWER);
            assertThatThrownBy(() -> stateMachine.transition(session, InterviewAction.START))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("ASKING + GO_NEXT 应抛出异常（ASKING 不接受 GO_NEXT）")
        void askingRejectsGoNext() {
            InterviewSession session = createSession(InterviewState.ASKING);
            assertThatThrownBy(() -> stateMachine.transition(session, InterviewAction.GO_NEXT))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("COMPLETED 终态不接受任何动作")
        void completedRejectsAll() {
            InterviewSession session = createSession(InterviewState.COMPLETED);
            assertThatThrownBy(() -> stateMachine.transition(session, InterviewAction.START))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("FAILED 终态不接受任何动作")
        void failedRejectsAll() {
            InterviewSession session = createSession(InterviewState.FAILED);
            assertThatThrownBy(() -> stateMachine.transition(session, InterviewAction.START))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("CANCELLED 终态不接受任何动作")
        void cancelledRejectsAll() {
            InterviewSession session = createSession(InterviewState.CANCELLED);
            assertThatThrownBy(() -> stateMachine.transition(session, InterviewAction.START))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("session 为 null 时抛出 IllegalArgumentException")
        void nullSessionThrowsIllegalArgumentException() {
            assertThatThrownBy(() -> stateMachine.transition(null, InterviewAction.START))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("session 不能为 null");
        }
    }

    // ── CANCEL / FAIL 通配转换 ──

    @Nested
    @DisplayName("CANCEL / FAIL 通配转换")
    class CancelFail {

        @Test
        @DisplayName("READY + CANCEL → CANCELLED")
        void readyToCancelled() {
            InterviewSession session = createSession(InterviewState.READY);
            stateMachine.transition(session, InterviewAction.CANCEL);
            assertThat(session.getStatus()).isEqualTo(InterviewState.CANCELLED.name());
            assertThat(session.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("ASKING + CANCEL → CANCELLED")
        void askingToCancelled() {
            InterviewSession session = createSession(InterviewState.ASKING);
            stateMachine.transition(session, InterviewAction.CANCEL);
            assertThat(session.getStatus()).isEqualTo(InterviewState.CANCELLED.name());
        }

        @Test
        @DisplayName("WAITING_ANSWER + FAIL → FAILED")
        void waitingAnswerToFailed() {
            InterviewSession session = createSession(InterviewState.WAITING_ANSWER);
            stateMachine.transition(session, InterviewAction.FAIL);
            assertThat(session.getStatus()).isEqualTo(InterviewState.FAILED.name());
            assertThat(session.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("EVALUATING + FAIL → FAILED")
        void evaluatingToFailed() {
            InterviewSession session = createSession(InterviewState.EVALUATING);
            stateMachine.transition(session, InterviewAction.FAIL);
            assertThat(session.getStatus()).isEqualTo(InterviewState.FAILED.name());
        }

        @Test
        @DisplayName("COMPLETED 终态不接受 CANCEL")
        void completedRejectsCancel() {
            InterviewSession session = createSession(InterviewState.COMPLETED);
            assertThatThrownBy(() -> stateMachine.transition(session, InterviewAction.CANCEL))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("FAILED 终态不接受 FAIL")
        void failedRejectsFail() {
            InterviewSession session = createSession(InterviewState.FAILED);
            assertThatThrownBy(() -> stateMachine.transition(session, InterviewAction.FAIL))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ── 分支判断 ──

    @Nested
    @DisplayName("分支判断: determinePostEvaluationAction")
    class Branching {

        @Test
        @DisplayName("第 1 题答完（共 3 题）→ GO_NEXT")
        void firstQuestionGoesNext() {
            InterviewSession session = createSession(InterviewState.EVALUATING);
            session.setCurrentQuestionIndex(1);
            session.setTotalQuestions(3);

            InterviewAction action = stateMachine.determinePostEvaluationAction(session);
            assertThat(action).isEqualTo(InterviewAction.GO_NEXT);
        }

        @Test
        @DisplayName("第 2 题答完（共 3 题）→ GO_NEXT")
        void secondQuestionGoesNext() {
            InterviewSession session = createSession(InterviewState.EVALUATING);
            session.setCurrentQuestionIndex(2);
            session.setTotalQuestions(3);

            InterviewAction action = stateMachine.determinePostEvaluationAction(session);
            assertThat(action).isEqualTo(InterviewAction.GO_NEXT);
        }

        @Test
        @DisplayName("第 3 题答完（共 3 题）→ GO_SUMMARIZE")
        void lastQuestionGoesSummarize() {
            InterviewSession session = createSession(InterviewState.EVALUATING);
            session.setCurrentQuestionIndex(3);
            session.setTotalQuestions(3);

            InterviewAction action = stateMachine.determinePostEvaluationAction(session);
            assertThat(action).isEqualTo(InterviewAction.GO_SUMMARIZE);
        }

        @Test
        @DisplayName("currentQuestionIndex 为 null 时默认 0 → GO_NEXT")
        void nullCurrentIndexDefaultsToZero() {
            InterviewSession session = createSession(InterviewState.EVALUATING);
            session.setCurrentQuestionIndex(null);
            session.setTotalQuestions(3);

            InterviewAction action = stateMachine.determinePostEvaluationAction(session);
            assertThat(action).isEqualTo(InterviewAction.GO_NEXT);
        }

        @Test
        @DisplayName("totalQuestions 为 null 时默认 5")
        void nullTotalQuestionsDefaultsToThree() {
            InterviewSession session = createSession(InterviewState.EVALUATING);
            session.setCurrentQuestionIndex(5);
            session.setTotalQuestions(null);

            InterviewAction action = stateMachine.determinePostEvaluationAction(session);
            assertThat(action).isEqualTo(InterviewAction.GO_SUMMARIZE);
        }
    }

    // ── canTransition ──

    @Nested
    @DisplayName("canTransition 校验")
    class CanTransition {

        @Test
        @DisplayName("合法转换返回 true")
        void validTransitionReturnsTrue() {
            InterviewSession session = createSession(InterviewState.READY);
            assertThat(stateMachine.canTransition(session, InterviewAction.START)).isTrue();
        }

        @Test
        @DisplayName("非法转换返回 false")
        void invalidTransitionReturnsFalse() {
            InterviewSession session = createSession(InterviewState.READY);
            assertThat(stateMachine.canTransition(session, InterviewAction.ANSWER_SUBMITTED)).isFalse();
        }

        @Test
        @DisplayName("终态不接受任何动作返回 false")
        void terminalStateReturnsFalse() {
            InterviewSession session = createSession(InterviewState.COMPLETED);
            assertThat(stateMachine.canTransition(session, InterviewAction.START)).isFalse();
        }
    }

    // ── markFailed / markCancelled ──

    @Nested
    @DisplayName("markFailed / markCancelled")
    class MarkMethods {

        @Test
        @DisplayName("markFailed 将状态设为 FAILED")
        void markFailedSetsFailed() {
            InterviewSession session = createSession(InterviewState.ASKING);
            stateMachine.markFailed(session);
            assertThat(session.getStatus()).isEqualTo(InterviewState.FAILED.name());
            assertThat(session.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("markCancelled 将状态设为 CANCELLED")
        void markCancelledSetsCancelled() {
            InterviewSession session = createSession(InterviewState.ASKING);
            stateMachine.markCancelled(session);
            assertThat(session.getStatus()).isEqualTo(InterviewState.CANCELLED.name());
            assertThat(session.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("markFailed 对已 FAILED 会话幂等，不抛异常")
        void markFailedIsIdempotent() {
            InterviewSession session = createSession(InterviewState.FAILED);
            assertDoesNotThrow(() -> stateMachine.markFailed(session));
            assertThat(session.getStatus()).isEqualTo(InterviewState.FAILED.name());
        }

        @Test
        @DisplayName("markCancelled 对已 CANCELLED 会话幂等，不抛异常")
        void markCancelledIsIdempotent() {
            InterviewSession session = createSession(InterviewState.CANCELLED);
            assertDoesNotThrow(() -> stateMachine.markCancelled(session));
            assertThat(session.getStatus()).isEqualTo(InterviewState.CANCELLED.name());
        }

        @Test
        @DisplayName("markFailed 对 COMPLETED 会话幂等，不抛异常")
        void markFailedOnCompletedIsIdempotent() {
            InterviewSession session = createSession(InterviewState.COMPLETED);
            assertDoesNotThrow(() -> stateMachine.markFailed(session));
            assertThat(session.getStatus()).isEqualTo(InterviewState.COMPLETED.name());
        }
    }

    // ── getNextState 防御性 null guard ──

    @Nested
    @DisplayName("getNextState null guard")
    class GetNextStateNullGuard {

        @Test
        @DisplayName("currentState 为 null 时返回 null")
        void nullCurrentStateReturnsNull() {
            assertThat(InterviewTransitionTable.getNextState(null, InterviewAction.START))
                    .isNull();
        }

        @Test
        @DisplayName("currentState 为 null 且 action 为 CANCEL 时返回 null")
        void nullCurrentStateWithCancelReturnsNull() {
            assertThat(InterviewTransitionTable.getNextState(null, InterviewAction.CANCEL))
                    .isNull();
        }
    }

    // ── 辅助方法 ──

    private InterviewSession createSession(InterviewState state) {
        InterviewSession session = new InterviewSession();
        session.setId(1L);
        session.setStatus(state.name());
        session.setCurrentQuestionIndex(0);
        session.setTotalQuestions(3);
        return session;
    }
}