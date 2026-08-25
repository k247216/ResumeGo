package com.resumego.interview.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.ai.AiClient;
import com.resumego.ai.AiClientSelector;
import com.resumego.ai.AiRequest;
import com.resumego.interview.InterviewMode;
import com.resumego.interview.QuestionSourceType;
import com.resumego.interview.context.InterviewContextSnapshot;
import com.resumego.interview.service.InterviewPromptBuilder;
import com.resumego.knowledge.dto.KnowledgeContentResponse;
import com.resumego.knowledge.KnowledgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识训练题目来源：从用户已选文档生成问题，每个问题带来源文档引用与支撑片段；
 * 找不到依据时明确标注"资料中未找到依据"，不生成虚假引用。
 */
@Component
public class KnowledgeTrainingQuestionSource implements InterviewQuestionSource {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeTrainingQuestionSource.class);

    public static final String LABEL_SOURCED = "知识来源";
    public static final String LABEL_NO_EVIDENCE = "资料中未找到依据";

    private final AiClientSelector aiClientSelector;
    private final InterviewPromptBuilder promptBuilder;
    private final KnowledgeService knowledgeService;
    private final ObjectMapper objectMapper;

    public KnowledgeTrainingQuestionSource(AiClientSelector aiClientSelector,
                                           InterviewPromptBuilder promptBuilder,
                                           KnowledgeService knowledgeService,
                                           ObjectMapper objectMapper) {
        this.aiClientSelector = aiClientSelector;
        this.promptBuilder = promptBuilder;
        this.knowledgeService = knowledgeService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(InterviewMode mode) {
        return mode == InterviewMode.KNOWLEDGE_TRAINING;
    }

    @Override
    public List<QuestionDraft> prepare(InterviewContextSnapshot snapshot, int count) {
        if (snapshot.knowledgeDocumentIds() == null || snapshot.knowledgeDocumentIds().isEmpty()) {
            throw new IllegalArgumentException("知识训练缺少已选资料");
        }

        Map<Long, String> documentContents = new LinkedHashMap<>();
        for (Long documentId : snapshot.knowledgeDocumentIds()) {
            try {
                KnowledgeContentResponse content = knowledgeService.getContent(documentId);
                documentContents.put(documentId, content.content());
            } catch (Exception e) {
                // 单个资料不可读时跳过，不阻塞其他资料出题
                log.warn("知识资料内容不可读，跳过出题来源: documentId={}, reason={}", documentId, e.getMessage());
            }
        }
        if (documentContents.isEmpty()) {
            throw new IllegalStateException("所选知识资料均不可读，无法开始知识训练");
        }

        String systemPrompt = promptBuilder.buildKnowledgeQuestionSystemPrompt();
        String userMessage = promptBuilder.buildKnowledgeQuestionUserPrompt(
                documentContents, count, snapshot.difficulty());

        AiRequest request = AiRequest.builder()
                .featureType("interview_knowledge_questions")
                .promptVersion(InterviewContextSnapshot.PROMPT_VERSION)
                .systemPrompt(systemPrompt)
                .userMessage(userMessage)
                .build();
        var result = aiClientSelector.getClient().invoke(request);
        if (!result.success()) {
            throw new IllegalStateException("知识训练题目生成失败，请稍后重试");
        }
        return parseDrafts(result.content(), snapshot.knowledgeDocumentIds());
    }

    /** 解析 AI 输出：无 quote 的题目明确标注"资料中未找到依据"，不伪造引用。 */
    List<QuestionDraft> parseDrafts(String content, List<Long> selectedDocumentIds) {
        List<QuestionDraft> drafts = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode questions = root.path("questions");
            for (JsonNode node : questions) {
                String text = node.path("questionText").asText("");
                if (text.isBlank()) {
                    continue;
                }
                long sourceDocumentId = node.path("sourceDocumentId").asLong(0);
                String quote = node.path("quote").asText("");
                boolean hasEvidence = sourceDocumentId > 0
                        && selectedDocumentIds.contains(sourceDocumentId)
                        && !quote.isBlank();
                drafts.add(new QuestionDraft(
                        text,
                        node.path("questionType").asText("深入"),
                        QuestionSourceType.AI_GENERATED,
                        hasEvidence ? "knowledge_doc:" + sourceDocumentId + ":" + quote : null,
                        hasEvidence ? LABEL_SOURCED : LABEL_NO_EVIDENCE
                ));
            }
        } catch (Exception e) {
            throw new IllegalStateException("知识训练题目输出解析失败", e);
        }
        if (drafts.isEmpty()) {
            throw new IllegalStateException("知识训练未产出可用题目");
        }
        return drafts;
    }
}
