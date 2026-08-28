package com.resumego.interview.source;

import com.resumego.interview.InterviewMode;
import com.resumego.interview.QuestionSourceType;
import com.resumego.interview.context.InterviewContextSnapshot;
import com.resumego.interview.repository.InterviewQuestionSetRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 面经题目来源：按题集顺序返回原题，不经 AI 改写；
 * 题量不足时只返回实际题数，不生成题冒充面经；
 * AI 追问由会话层生成并单独标记 AI_FOLLOW_UP。
 */
@Component
public class ExperienceQuestionSource implements InterviewQuestionSource {

    /** 用户可见来源标签：真实题 vs 练习题 */
    public static final String LABEL_REAL_QUESTION = "真实面经原题";
    public static final String LABEL_PRACTICE = "练习题";

    private final InterviewQuestionSetRepository questionSetRepository;

    public ExperienceQuestionSource(InterviewQuestionSetRepository questionSetRepository) {
        this.questionSetRepository = questionSetRepository;
    }

    @Override
    public boolean supports(InterviewMode mode) {
        return mode == InterviewMode.EXPERIENCE_SIMULATION;
    }

    @Override
    public List<QuestionDraft> prepare(InterviewContextSnapshot snapshot, int count) {
        if (snapshot.questionSetId() == null) {
            throw new IllegalArgumentException("面经模拟缺少题集");
        }
        QuestionSourceType sourceType = QuestionSourceType.valueOf(snapshot.questionSetSourceType());
        // 只有用户手录与导入面经可标"真实题目"；生成练习题必须显著标注
        String provenance = sourceType == QuestionSourceType.GENERATED_PRACTICE
                ? LABEL_PRACTICE
                : LABEL_REAL_QUESTION;

        List<String> questions = questionSetRepository.findQuestionTexts(snapshot.questionSetId());
        questions = applyQuestionOrder(questions, snapshot.questionOrder());
        int limit = Math.min(count, questions.size());
        return questions.subList(0, limit).stream()
                .map(text -> new QuestionDraft(text, "面经原题", sourceType,
                        "question_set:" + snapshot.questionSetId(), provenance))
                .toList();
    }

    /** 面试开始时使用快照中的完整排列，历史记录不会受题集后续排序影响。 */
    private List<String> applyQuestionOrder(List<String> questions, List<Integer> order) {
        if (order == null || order.isEmpty()) return questions;
        if (order.size() != questions.size()) {
            throw new IllegalArgumentException("题目顺序与题集题数不一致");
        }
        boolean[] seen = new boolean[questions.size()];
        List<String> ordered = new java.util.ArrayList<>(questions.size());
        for (Integer index : order) {
            if (index == null || index < 0 || index >= questions.size() || seen[index]) {
                throw new IllegalArgumentException("题目顺序不是有效的完整排列");
            }
            seen[index] = true;
            ordered.add(questions.get(index));
        }
        return List.copyOf(ordered);
    }
}
