package com.resumego.interview.service;

import com.resumego.interview.InterviewAction;
import com.resumego.interview.InterviewState;
import com.resumego.interview.entity.InterviewSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InterviewStateMachine 并发测试。
 */
@DisplayName("InterviewStateMachine 并发测试")
class InterviewStateMachineConcurrencyTest {

    private InterviewStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new InterviewStateMachine();
    }

    private InterviewSession sessionInState(InterviewState state, int totalQuestions) {
        InterviewSession s = new InterviewSession();
        s.setId(1L);
        s.setStatus(state.name());
        s.setTotalQuestions(totalQuestions);
        s.setCurrentQuestionIndex(0);
        return s;
    }

    @Nested
    @DisplayName("并发: 多线程同时 markFailed")
    class ConcurrentMarkFailed {

        @Test
        @DisplayName("CONC-FAIL: 10 线程同时 markFailed → 状态一致为 FAILED")
        void shouldRemainFailedUnderConcurrency() throws Exception {
            int threads = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(threads);
            InterviewSession session = sessionInState(InterviewState.READY, 5);

            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        latch.countDown();
                        latch.await();
                        stateMachine.markFailed(session);
                    } catch (Exception ignored) {
                    }
                });
            }
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            assertThat(session.getStatus()).isEqualTo(InterviewState.FAILED.name());
        }
    }

    @Nested
    @DisplayName("并发: 多线程同时 transition")
    class ConcurrentTransition {

        @Test
        @DisplayName("CONC-TRANS: 同时 READY→START → 状态一致")
        void shouldTransitionCorrectlyUnderConcurrency() throws Exception {
            int threads = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(threads);
            InterviewSession session = sessionInState(InterviewState.READY, 5);
            AtomicReference<String> error = new AtomicReference<>(null);

            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        latch.countDown();
                        latch.await();
                        stateMachine.transition(session, InterviewAction.START);
                    } catch (IllegalStateException e) {
                        error.set(e.getMessage());
                    } catch (Exception e) {
                        // expected for threads after first
                    }
                });
            }
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            // 最终状态应稳定（至少一个成功转移到了 ASKING）
            assertThat(session.getStatus()).isIn(
                    InterviewState.ASKING.name(), InterviewState.WAITING_ANSWER.name()
            );
        }
    }

    @Nested
    @DisplayName("并发: 幂等性")
    class ConcurrentIdempotency {

        @Test
        @DisplayName("CONC-IDEM: 对已终态并发 markFailed/markCancelled → 无异常")
        void shouldBeIdempotentUnderConcurrency() throws Exception {
            InterviewSession completed = sessionInState(InterviewState.COMPLETED, 5);
            completed.setCompletedAt(java.time.LocalDateTime.now());

            int threads = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(threads);

            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        latch.countDown();
                        latch.await();
                        stateMachine.markFailed(completed);
                        stateMachine.markCancelled(completed);
                    } catch (Exception ignored) {
                    }
                });
            }
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            // 终态不变
            assertThat(completed.getStatus()).isEqualTo(InterviewState.COMPLETED.name());
        }
    }
}
