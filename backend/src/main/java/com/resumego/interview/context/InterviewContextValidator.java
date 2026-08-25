package com.resumego.interview.context;

import com.resumego.interview.InterviewMode;

/**
 * 模式上下文校验器：校验该模式必需输入全部属于当前用户且可用，
 * 并构造不可变开始快照。校验失败抛 IllegalArgumentException（400）。
 */
public interface InterviewContextValidator {

    boolean supports(InterviewMode mode);

    InterviewContextSnapshot validate(InterviewStartContext context);
}
