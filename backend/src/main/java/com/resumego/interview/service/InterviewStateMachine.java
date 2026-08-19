package com.resumego.interview.service;

import com.resumego.interview.InterviewAction;
import com.resumego.interview.InterviewState;
import com.resumego.interview.InterviewTransitionTable;
import com.resumego.interview.entity.InterviewSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;



@Service
public class InterviewStateMachine {

    private static final Logger log = LoggerFactory.getLogger(InterviewStateMachine.class);
    private static final int DEFAULT_TOTAL_QUESTIONS = 5;

    /**
     * 执行状态转换。
     *
     * @param session 当前面试会话（会被修改），不能为 null
     * @param action  触发的动作
     * @throws IllegalArgumentException 如果 session 为 null
     * @throws IllegalStateException    如果当前状态为终态或转换不合法
     */
    public void transition(InterviewSession session, InterviewAction action) {
        if (session == null) {
            throw new IllegalArgumentException("session 不能为 null");
        }

        InterviewState currentState = parseState(session.getStatus());
        InterviewState nextState = InterviewTransitionTable.getNextState(currentState, action);

        if (nextState == null) {
            throw new IllegalStateException(
                    String.format("非法的状态转换: sessionId=%s, status=%s, action=%s",
                            session.getId(), currentState, action));
        }

        log.info("状态转换: sessionId={}, {} --{}--> {}",
                session.getId(), currentState, action, nextState);

        // 执行转换
        session.setStatus(nextState.name());

        // 开始面试时记录开始时间
        if (action == InterviewAction.START) {
            session.setStartedAt(LocalDateTime.now());
        }

        // 到达终态时记录完成时间
        if (InterviewTransitionTable.isTerminal(nextState)) {
            session.setCompletedAt(LocalDateTime.now());
        }
    }

    /**
     * 在 EVALUATING 状态后，根据当前题号判断下一步动作。
     *
     * @param session 当前会话（status 应为 EVALUATING）
     * @return 如果是最后一题返回 GO_SUMMARIZE，否则返回 GO_NEXT
     */
    public InterviewAction determinePostEvaluationAction(InterviewSession session) {
        int currentIndex = getCurrentQuestionIndex(session);
        int total = getTotalQuestions(session);

        if (currentIndex >= total) {
            return InterviewAction.GO_SUMMARIZE;
        }
        return InterviewAction.GO_NEXT;
    }

    /**
     * 标记会话失败（AI 调用失败且重试耗尽）。
     * 幂等：如果会话已是终态，静默返回不做任何操作。
     */
    public void markFailed(InterviewSession session) {
        InterviewState currentState = parseState(session.getStatus());
        if (InterviewTransitionTable.isTerminal(currentState)) {
            log.info("会话已处于终态，跳过 markFailed: sessionId={}, status={}",
                    session.getId(), currentState);
            return;
        }
        transition(session, InterviewAction.FAIL);
    }

    /**
     * 取消会话。
     * 幂等：如果会话已是终态，静默返回不做任何操作。
     */
    public void markCancelled(InterviewSession session) {
        InterviewState currentState = parseState(session.getStatus());
        if (InterviewTransitionTable.isTerminal(currentState)) {
            log.info("会话已处于终态，跳过 markCancelled: sessionId={}, status={}",
                    session.getId(), currentState);
            return;
        }
        transition(session, InterviewAction.CANCEL);
    }

    /**
     * 校验当前状态是否允许指定动作。
     */
    public boolean canTransition(InterviewSession session, InterviewAction action) {
        InterviewState currentState = parseState(session.getStatus());
        return InterviewTransitionTable.getNextState(currentState, action) != null;
    }

    // ── 私有辅助方法 ──

    private InterviewState parseState(String status) {
        try {
            return InterviewState.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalStateException("未知的会话状态: " + status, e);
        }
    }

    private int getCurrentQuestionIndex(InterviewSession session) {
        return session.getCurrentQuestionIndex() != null
                ? session.getCurrentQuestionIndex() : 0;
    }

    private int getTotalQuestions(InterviewSession session) {
        return session.getTotalQuestions() != null
                ? session.getTotalQuestions() : DEFAULT_TOTAL_QUESTIONS;
    }
}