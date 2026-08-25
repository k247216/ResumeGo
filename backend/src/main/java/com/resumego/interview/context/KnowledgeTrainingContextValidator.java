package com.resumego.interview.context;

import com.resumego.common.CurrentUser;
import com.resumego.interview.InterviewMode;
import com.resumego.knowledge.KnowledgeDocument;
import com.resumego.knowledge.KnowledgeRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识训练校验：只要求当前用户可用的 Knowledge Document，不强制岗位/简历。
 */
@Component
public class KnowledgeTrainingContextValidator implements InterviewContextValidator {

    private final KnowledgeRepository knowledgeRepository;

    public KnowledgeTrainingContextValidator(KnowledgeRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
    }

    @Override
    public boolean supports(InterviewMode mode) {
        return mode == InterviewMode.KNOWLEDGE_TRAINING;
    }

    @Override
    public InterviewContextSnapshot validate(InterviewStartContext context) {
        if (!(context instanceof InterviewStartContext.KnowledgeTraining training)) {
            throw new IllegalArgumentException("知识训练上下文类型不正确");
        }
        if (training.knowledgeDocumentIds() == null || training.knowledgeDocumentIds().isEmpty()) {
            throw new IllegalArgumentException("知识训练至少选择一份资料");
        }

        List<Long> ids = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        for (Long documentId : training.knowledgeDocumentIds()) {
            KnowledgeDocument document = knowledgeRepository.findById(CurrentUser.DEMO_USER_ID, documentId)
                    .orElseThrow(() -> new IllegalArgumentException("知识资料不存在: " + documentId));
            ids.add(document.id());
            titles.add(document.title());
        }

        return new InterviewContextSnapshot(
                InterviewContextSnapshot.CONTRACT_VERSION,
                InterviewMode.KNOWLEDGE_TRAINING.name(),
                null,
                null,
                null,
                null,
                null,
                null,
                ids,
                titles,
                null,
                null,
                null,
                null,
                null,
                training.questionCount(),
                training.focusTags(),
                training.difficulty(),
                null,
                InterviewContextSnapshot.PROMPT_VERSION,
                InterviewContextSnapshot.OUTPUT_SCHEMA_VERSION
        );
    }
}
