package com.resumego.interview;

/**
 * 面试题目来源类型。AI 追问必须标为 AI_FOLLOW_UP，不得混入原始题集。
 */
public enum QuestionSourceType {
    AI_GENERATED,
    SYSTEM_DEFINED,
    USER_MANUAL,
    IMPORTED_EXPERIENCE,
    GENERATED_PRACTICE,
    AI_FOLLOW_UP
}
