package com.resumego.interview;

public enum InterviewAction {

    /** 开始面试：READY → ASKING */
    START,

    /** 问题已生成就绪：ASKING → WAITING_ANSWER */
    QUESTION_READY,

    /** 用户提交回答：WAITING_ANSWER → EVALUATING */
    ANSWER_SUBMITTED,

    /** 非最后一题，继续下一题：EVALUATING → ASKING */
    GO_NEXT,

    /** 最后一题，进入总结：EVALUATING → SUMMARIZING */
    GO_SUMMARIZE,

    /** 总结生成完毕：SUMMARIZING → COMPLETED */
    SUMMARY_READY,

    /** 用户主动取消：任意非终态 → CANCELLED */
    CANCEL,

    /** 系统异常（AI 调用失败且重试耗尽）：任意非终态 → FAILED */
    FAIL
}