package com.resumego.interview.source;

import com.resumego.interview.InterviewMode;
import com.resumego.interview.QuestionSourceType;
import com.resumego.interview.context.InterviewContextSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 岗位模拟题目来源：保持既有行为不变——题目由确定性状态机内 AI 实时生成，
 * 来源准备职责仅提供 JD/简历/公司画像材料（InterviewService 既有逻辑），
 * 因此 prepare 返回空列表；所有 AI 生成题标 AI_GENERATED，追问标 AI_FOLLOW_UP（会话层）。
 */
@Component
public class RoleBasedQuestionSource implements InterviewQuestionSource {

    @Override
    public boolean supports(InterviewMode mode) {
        return mode == InterviewMode.ROLE_BASED;
    }

    @Override
    public List<QuestionDraft> prepare(InterviewContextSnapshot snapshot, int count) {
        return List.of();
    }
}
