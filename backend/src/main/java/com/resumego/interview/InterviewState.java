package com.resumego.interview;

public enum InterviewState {

    /** 初始空闲态：面试会话已创建，等待开始 */
    READY,

    /** 提问状态：AI 正在生成或已生成问题，等待展示给用户 */
    ASKING,

    /** 等待答题状态：问题已展示，等待用户提交回答 */
    WAITING_ANSWER,

    /** 评估答案状态：AI 正在对回答进行结构化评价 */
    EVALUATING,

    /** 总结状态：AI 正在生成面试总结报告 */
    SUMMARIZING,

    /** 正常完整结束 */
    COMPLETED,

    /** 用户主动取消 */
    CANCELLED,

    /** 系统异常终止（AI 调用失败且重试耗尽） */
    FAILED
}