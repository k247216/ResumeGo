package com.resumego.interview;

/**
 * 面试训练模式（冻结契约：三种且仅三种，创建后不可修改）。
 */
public enum InterviewMode {
    /** 岗位模拟：要求 Pipeline、Resume Version 和 persona */
    ROLE_BASED,
    /** 知识训练：只要求 Knowledge Document */
    KNOWLEDGE_TRAINING,
    /** 面经模拟：只使用本地题集 */
    EXPERIENCE_SIMULATION
}
