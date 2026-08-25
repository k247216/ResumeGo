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
        int limit = Math.min(count, questions.size());
        return questions.subList(0, limit).stream()
                .map(text -> new QuestionDraft(text, "面经原题", sourceType,
                        "question_set:" + snapshot.questionSetId(), provenance))
                .toList();
    }
}
