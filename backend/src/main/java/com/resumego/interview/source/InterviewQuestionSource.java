package com.resumego.interview.source;

import com.resumego.interview.InterviewMode;
import com.resumego.interview.context.InterviewContextSnapshot;

import java.util.List;

/**
 * 模式化题目来源：按模式准备题目草稿。
 * 岗位模式保持既有 AI 实时出题与确定性状态机不变；知识/面经模式由来源适配器准备题目。
 */
public interface InterviewQuestionSource {

    boolean supports(InterviewMode mode);

    /**
     * 准备题目草稿。
     * 面经模式按题集顺序返回原题（不经 AI 改写，题量不足不生成题冒充面经）；
     * 知识模式从已选文档生成并带来源引用；岗位模式返回空（题目由状态机内 AI 实时生成）。
     */
    List<QuestionDraft> prepare(InterviewContextSnapshot snapshot, int count);
}
