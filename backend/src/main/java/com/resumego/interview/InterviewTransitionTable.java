package com.resumego.interview;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class InterviewTransitionTable {

    private static final Map<InterviewState, Map<InterviewAction, InterviewState>> transitions;

    static {
        Map<InterviewState, Map<InterviewAction, InterviewState>> table = new EnumMap<>(InterviewState.class);

        // ── READY ──
        table.put(InterviewState.READY, Map.of(
                InterviewAction.START, InterviewState.ASKING
        ));

        // ── ASKING ──
        table.put(InterviewState.ASKING, Map.of(
                InterviewAction.QUESTION_READY, InterviewState.WAITING_ANSWER
        ));

        // ── WAITING_ANSWER ──
        table.put(InterviewState.WAITING_ANSWER, Map.of(
                InterviewAction.ANSWER_SUBMITTED, InterviewState.EVALUATING
        ));

        // ── EVALUATING ──
        table.put(InterviewState.EVALUATING, Map.of(
                InterviewAction.GO_NEXT, InterviewState.ASKING,
                InterviewAction.GO_SUMMARIZE, InterviewState.SUMMARIZING
        ));

        // ── SUMMARIZING ──
        table.put(InterviewState.SUMMARIZING, Map.of(
                InterviewAction.SUMMARY_READY, InterviewState.COMPLETED
        ));

        transitions = Collections.unmodifiableMap(table);
    }

    private InterviewTransitionTable() {
        // 工具类，禁止实例化
    }

    /**
     * 查询转换表，返回 (当前状态 + 动作) 对应的下一状态。
     *
     * @param currentState 当前状态
     * @param action       触发的动作
     * @return 下一状态；如果当前状态是终态或转换不合法，返回 {@code null}
     */
    public static InterviewState getNextState(InterviewState currentState, InterviewAction action) {
        if (currentState == null) {
            return null;
        }

        // CANCEL 和 FAIL 可从任意非终态触发
        if (action == InterviewAction.CANCEL && !isTerminal(currentState)) {
            return InterviewState.CANCELLED;
        }
        if (action == InterviewAction.FAIL && !isTerminal(currentState)) {
            return InterviewState.FAILED;
        }

        Map<InterviewAction, InterviewState> actionMap = transitions.get(currentState);
        if (actionMap == null) {
            return null;
        }
        return actionMap.get(action);
    }

    /**
     * 判断是否为终态（不再接受任何动作）。
     */
    public static boolean isTerminal(InterviewState state) {
        return state == InterviewState.COMPLETED
                || state == InterviewState.CANCELLED
                || state == InterviewState.FAILED;
    }
}