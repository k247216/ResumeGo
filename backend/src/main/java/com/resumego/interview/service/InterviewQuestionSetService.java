package com.resumego.interview.service;

import com.resumego.common.CurrentUser;
import com.resumego.interview.QuestionSourceType;
import com.resumego.interview.dto.InterviewQuestionSetRequest;
import com.resumego.interview.dto.InterviewQuestionSetResponse;
import com.resumego.interview.dto.InterviewQuestionSetSourcePreviewResponse;
import com.resumego.knowledge.KnowledgeClassificationService;
import com.resumego.knowledge.KnowledgeService;
import com.resumego.knowledge.dto.KnowledgeContentResponse;
import com.resumego.knowledge.dto.KnowledgeDocumentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import com.resumego.interview.repository.InterviewQuestionSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 面经题集服务：用户独立本地资产。来源说明只是用户声明文本，不做网络抓取；
 * 日志不记录题目全文。
 */
@Service
public class InterviewQuestionSetService {

    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_QUESTION_LENGTH = 1000;
    private static final int MAX_QUESTION_COUNT = 100;
    private static final int MAX_SOURCE_NOTE_LENGTH = 500;
    private static final int MAX_COMPANY_NAME_LENGTH = 120;
    private static final int MAX_TARGET_ROLE_LENGTH = 120;
    private static final int MAX_COMPANY_ICON_KEY_LENGTH = 80;
    private static final String REAL_EXPERIENCE_CATEGORY = "真实面经";

    private final InterviewQuestionSetRepository repository;
    private final KnowledgeService knowledgeService;
    private final KnowledgeClassificationService classificationService;
    private final InterviewExperienceParser experienceParser;

    /** 兼容不需要知识库桥接的单元测试与旧调用方。 */
    public InterviewQuestionSetService(InterviewQuestionSetRepository repository) {
        this(repository, null, null);
    }

    @Autowired
    public InterviewQuestionSetService(InterviewQuestionSetRepository repository,
                                       KnowledgeService knowledgeService,
                                       KnowledgeClassificationService classificationService) {
        this.repository = repository;
        this.knowledgeService = knowledgeService;
        this.classificationService = classificationService;
        this.experienceParser = new InterviewExperienceParser();
    }

    @Transactional
    public InterviewQuestionSetResponse create(InterviewQuestionSetRequest request) {
        String title = validateTitle(request);
        QuestionSourceType sourceType = validateSourceType(request);
        String sourceNote = validateSourceNote(request);
        String companyName = validateOptionalText(request == null ? null : request.companyName(), "公司名称", MAX_COMPANY_NAME_LENGTH);
        String targetRole = validateOptionalText(request == null ? null : request.targetRole(), "目标岗位", MAX_TARGET_ROLE_LENGTH);
        String companyIconKey = validateOptionalText(request == null ? null : request.companyIconKey(), "公司图标标识", MAX_COMPANY_ICON_KEY_LENGTH);
        List<String> questions = validateQuestions(request);

        long setId = companyName == null && targetRole == null && companyIconKey == null
                ? repository.createSet(CurrentUser.DEMO_USER_ID, title, sourceType, sourceNote, questions)
                : repository.createSet(CurrentUser.DEMO_USER_ID, title, sourceType, sourceNote,
                companyName, targetRole, companyIconKey, questions);
        return toDetailResponse(repository.findSetById(CurrentUser.DEMO_USER_ID, setId), setId);
    }

    /** 列表：不含题目正文；已关联知识库的题集先同步当前资料。 */
    @Transactional
    public List<InterviewQuestionSetResponse> list() {
        return repository.findAllSets(CurrentUser.DEMO_USER_ID).stream()
                .map(this::refreshLinkedSet)
                .map(this::toMetaResponse)
                .toList();
    }

    @Transactional
    public InterviewQuestionSetResponse get(long setId) {
        InterviewQuestionSetRepository.QuestionSetRow row = requireOwnedSet(setId);
        row = refreshLinkedSet(row);
        return toDetailResponse(row, setId);
    }

    /**
     * 将“真实面经”分类下的知识库资料登记为可练习题集。
     * 仅消费资料中明确列出的题目与 front matter 元数据，不调用 AI、不从标题推断公司，
     * 且同一资料只物化一次。
     */
    @Transactional
    public InterviewQuestionSetResponse createFromKnowledgeDocument(long documentId) {
        if (knowledgeService == null || classificationService == null) {
            throw new IllegalStateException("知识库桥接服务尚未配置");
        }
        if (!classificationService.isDocumentUnderCategoryNamed(documentId, REAL_EXPERIENCE_CATEGORY)) {
            throw new IllegalArgumentException("面经资料必须归入“真实面经”文件夹");
        }

        KnowledgeDocumentResponse document = knowledgeService.get(documentId);
        if (!"COMPLETED".equals(document.processingStatus())) {
            throw new IllegalStateException("面经资料尚未完成文本提取");
        }
        KnowledgeContentResponse content = knowledgeService.getContent(documentId);
        InterviewExperienceParser.Parsed parsed = experienceParser.parse(document.title(), content.content());
        String companyName = validateOptionalText(parsed.companyName(), "公司名称", MAX_COMPANY_NAME_LENGTH);
        String targetRole = validateOptionalText(parsed.targetRole(), "目标岗位", MAX_TARGET_ROLE_LENGTH);
        String companyIconKey = validateOptionalText(parsed.companyIconKey(), "公司图标标识", MAX_COMPANY_ICON_KEY_LENGTH);
        QuestionSourceType sourceType = "NOTE".equalsIgnoreCase(document.sourceType())
                ? QuestionSourceType.USER_MANUAL : QuestionSourceType.IMPORTED_EXPERIENCE;
        String sourceNote = validateSourceNote("知识库资料：" + document.title());

        var existingId = repository.findSetIdBySourceDocument(CurrentUser.DEMO_USER_ID, documentId);
        if (existingId.isPresent()) {
            InterviewQuestionSetRepository.QuestionSetRow existing = requireOwnedSet(existingId.get());
            if (existing.archived()) {
                throw new IllegalStateException("该面经题集已归档，请先恢复后再练习");
            }
            companyIconKey = effectiveCompanyIconKey(existing, companyName, companyIconKey);
            // 知识库是唯一事实来源：同一资料再次进入练习时刷新题目、公司、岗位和 Logo。
            repository.replaceSet(CurrentUser.DEMO_USER_ID, existing.id(), document.title(), sourceType, sourceNote,
                    companyName, targetRole, companyIconKey, parsed.questions());
            return toDetailResponse(repository.findSetById(CurrentUser.DEMO_USER_ID, existing.id()), existing.id());
        }

        long setId = repository.createSet(CurrentUser.DEMO_USER_ID, document.title(), sourceType, sourceNote,
                companyName, targetRole, companyIconKey, documentId, parsed.questions());
        return toDetailResponse(repository.findSetById(CurrentUser.DEMO_USER_ID, setId), setId);
    }

    /**
     * 只检查知识库面经格式，不创建题集、不修改知识库正文。
     * 知识库页面用它反馈“已识别几道题”或具体格式问题，真正物化仍只发生在真题演练选择时。
     */
    public InterviewQuestionSetSourcePreviewResponse previewKnowledgeDocument(long documentId) {
        if (knowledgeService == null || classificationService == null) {
            throw new IllegalStateException("知识库桥接服务尚未配置");
        }
        if (!classificationService.isDocumentUnderCategoryNamed(documentId, REAL_EXPERIENCE_CATEGORY)) {
            return InterviewQuestionSetSourcePreviewResponse.invalid(documentId, "资料必须归入“真实面经”文件夹");
        }

        KnowledgeDocumentResponse document = knowledgeService.get(documentId);
        if (!"COMPLETED".equals(document.processingStatus())) {
            return InterviewQuestionSetSourcePreviewResponse.processing(documentId, "正文尚未完成提取，完成后会自动识别题目");
        }
        try {
            KnowledgeContentResponse content = knowledgeService.getContent(documentId);
            InterviewExperienceParser.Parsed parsed = experienceParser.parse(document.title(), content.content());
            return new InterviewQuestionSetSourcePreviewResponse(
                    documentId, "READY", parsed.questions().size(),
                    "已按真实面经格式识别，选择真题演练后即可使用",
                    parsed.companyName(), parsed.targetRole(), parsed.companyIconKey());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return InterviewQuestionSetSourcePreviewResponse.invalid(documentId, e.getMessage());
        }
    }

    /** 原子替换元数据与题目；归档题集不可更新。 */
    @Transactional
    public InterviewQuestionSetResponse update(long setId, InterviewQuestionSetRequest request) {
        InterviewQuestionSetRepository.QuestionSetRow existing = requireOwnedSet(setId);
        if (existing.archived()) {
            throw new IllegalStateException("已归档题集不可修改");
        }
        String title = validateTitle(request);
        QuestionSourceType sourceType = validateSourceType(request);
        String sourceNote = validateSourceNote(request);
        String companyName = validateOptionalText(request == null ? null : request.companyName(), "公司名称", MAX_COMPANY_NAME_LENGTH);
        String targetRole = validateOptionalText(request == null ? null : request.targetRole(), "目标岗位", MAX_TARGET_ROLE_LENGTH);
        String companyIconKey = validateOptionalText(request == null ? null : request.companyIconKey(), "公司图标标识", MAX_COMPANY_ICON_KEY_LENGTH);
        List<String> questions = validateQuestions(request);

        if (companyName == null && targetRole == null && companyIconKey == null) {
            repository.replaceSet(CurrentUser.DEMO_USER_ID, setId, title, sourceType, sourceNote, questions);
        } else {
            repository.replaceSet(CurrentUser.DEMO_USER_ID, setId, title, sourceType, sourceNote,
                    companyName, targetRole, companyIconKey, questions);
        }
        return toDetailResponse(repository.findSetById(CurrentUser.DEMO_USER_ID, setId), setId);
    }

    /** 归档：阻止开始新面试，保留历史引用；重复归档无副作用。 */
    @Transactional
    public InterviewQuestionSetResponse archive(long setId) {
        InterviewQuestionSetRepository.QuestionSetRow row = requireOwnedSet(setId);
        if (!row.archived()) {
            repository.updateArchivedAt(CurrentUser.DEMO_USER_ID, setId, LocalDateTime.now());
        }
        return toDetailResponse(repository.findSetById(CurrentUser.DEMO_USER_ID, setId), setId);
    }

    private InterviewQuestionSetRepository.QuestionSetRow requireOwnedSet(long setId) {
        InterviewQuestionSetRepository.QuestionSetRow row =
                repository.findSetById(CurrentUser.DEMO_USER_ID, setId);
        if (row == null) {
            throw new NoSuchElementException("面经题集不存在");
        }
        return row;
    }

    /**
     * 知识库资料是面经题集的唯一事实来源。列表或详情打开时重新读取已完成的资料，
     * 以便题目、公司、岗位和图标随资料修改更新；若资料暂时不可用，则保留上次可用题集，
     * 不影响历史面试快照，也不凭空生成题目。
     */
    private InterviewQuestionSetRepository.QuestionSetRow refreshLinkedSet(
            InterviewQuestionSetRepository.QuestionSetRow row) {
        if (row == null || row.archived() || row.sourceDocumentId() == null
                || knowledgeService == null || classificationService == null) {
            return row;
        }
        long documentId = row.sourceDocumentId();
        try {
            if (!classificationService.isDocumentUnderCategoryNamed(documentId, REAL_EXPERIENCE_CATEGORY)) {
                return row;
            }
            KnowledgeDocumentResponse document = knowledgeService.get(documentId);
            if (!"COMPLETED".equals(document.processingStatus())) {
                return row;
            }
            KnowledgeContentResponse content = knowledgeService.getContent(documentId);
            InterviewExperienceParser.Parsed parsed = experienceParser.parse(document.title(), content.content());
            String companyName = validateOptionalText(parsed.companyName(), "公司名称", MAX_COMPANY_NAME_LENGTH);
            String targetRole = validateOptionalText(parsed.targetRole(), "目标岗位", MAX_TARGET_ROLE_LENGTH);
            String companyIconKey = validateOptionalText(parsed.companyIconKey(), "公司图标标识", MAX_COMPANY_ICON_KEY_LENGTH);
            companyIconKey = effectiveCompanyIconKey(row, companyName, companyIconKey);
            QuestionSourceType sourceType = "NOTE".equalsIgnoreCase(document.sourceType())
                    ? QuestionSourceType.USER_MANUAL : QuestionSourceType.IMPORTED_EXPERIENCE;
            String sourceNote = validateSourceNote("知识库资料：" + document.title());
            repository.replaceSet(CurrentUser.DEMO_USER_ID, row.id(), document.title(), sourceType, sourceNote,
                    companyName, targetRole, companyIconKey, parsed.questions());
            InterviewQuestionSetRepository.QuestionSetRow refreshed =
                    repository.findSetById(CurrentUser.DEMO_USER_ID, row.id());
            return refreshed == null ? row : refreshed;
        } catch (IllegalArgumentException | IllegalStateException | NoSuchElementException ignored) {
            // 暂时无法解析时保留上一次题集，避免列表消失；知识库页面会展示具体格式状态。
            return row;
        }
    }

    /** 公司名称变更而 icon 仍是旧值时，让前端按新公司自动匹配 Logo；用户改了 icon 则保留显式值。 */
    private String effectiveCompanyIconKey(InterviewQuestionSetRepository.QuestionSetRow existing,
                                           String companyName, String requestedIconKey) {
        if (existing != null && existing.companyName() != null && companyName != null
                && !existing.companyName().equals(companyName)
                && java.util.Objects.equals(existing.companyIconKey(), requestedIconKey)) {
            return null;
        }
        return requestedIconKey;
    }

    private String validateTitle(InterviewQuestionSetRequest request) {
        String title = request == null || request.title() == null ? "" : request.title().trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("题集名称不能为空");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("题集名称过长，最大 " + MAX_TITLE_LENGTH + " 字符");
        }
        return title;
    }

    private QuestionSourceType validateSourceType(InterviewQuestionSetRequest request) {
        QuestionSourceType sourceType = request == null ? null : request.sourceType();
        if (sourceType == null) {
            throw new IllegalArgumentException("题集来源类型不能为空");
        }
        return sourceType;
    }

    private String validateSourceNote(InterviewQuestionSetRequest request) {
        String note = request == null || request.sourceNote() == null ? null : request.sourceNote().trim();
        if (note != null && note.isEmpty()) {
            note = null;
        }
        if (note != null && note.length() > MAX_SOURCE_NOTE_LENGTH) {
            throw new IllegalArgumentException("来源说明过长，最大 " + MAX_SOURCE_NOTE_LENGTH + " 字符");
        }
        return note;
    }

    private String validateSourceNote(String note) {
        String normalized = note == null ? null : note.trim();
        if (normalized != null && normalized.isEmpty()) normalized = null;
        if (normalized != null && normalized.length() > MAX_SOURCE_NOTE_LENGTH) {
            throw new IllegalArgumentException("来源说明过长，最大 " + MAX_SOURCE_NOTE_LENGTH + " 字符");
        }
        return normalized;
    }

    private List<String> validateQuestions(InterviewQuestionSetRequest request) {
        List<String> questions = request == null ? null : request.questions();
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("题集至少包含一道题目");
        }
        if (questions.size() > MAX_QUESTION_COUNT) {
            throw new IllegalArgumentException("题集最多 " + MAX_QUESTION_COUNT + " 道题目");
        }
        List<String> normalized = new java.util.ArrayList<>(questions.size());
        for (int index = 0; index < questions.size(); index++) {
            String question = questions.get(index) == null ? "" : questions.get(index).trim();
            if (question.isEmpty()) {
                throw new IllegalArgumentException("第 " + (index + 1) + " 题内容不能为空");
            }
            if (question.length() > MAX_QUESTION_LENGTH) {
                throw new IllegalArgumentException("第 " + (index + 1) + " 题超过 " + MAX_QUESTION_LENGTH + " 字符");
            }
            normalized.add(question);
        }
        return normalized;
    }

    private String validateOptionalText(String raw, String label, int maxLength) {
        String value = raw == null ? null : raw.trim();
        if (value != null && value.isEmpty()) value = null;
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(label + "过长，最大 " + maxLength + " 字符");
        }
        return value;
    }

    private InterviewQuestionSetResponse toMetaResponse(InterviewQuestionSetRepository.QuestionSetRow row) {
        return new InterviewQuestionSetResponse(
                row.id(), row.title(), row.sourceType(), row.sourceNote(), row.companyName(),
                row.targetRole(), row.companyIconKey(), row.sourceDocumentId(), row.questionCount(),
                row.archived(), null,
                row.createdAt(), row.updatedAt(), null);
    }

    private InterviewQuestionSetResponse toDetailResponse(
            InterviewQuestionSetRepository.QuestionSetRow row, long setId) {
        if (row == null) {
            throw new NoSuchElementException("面经题集不存在");
        }
        List<String> texts = repository.findQuestionTexts(setId);
        List<InterviewQuestionSetResponse.QuestionItem> indexed = new java.util.ArrayList<>(texts.size());
        for (int index = 0; index < texts.size(); index++) {
            indexed.add(new InterviewQuestionSetResponse.QuestionItem(index, texts.get(index)));
        }
        return new InterviewQuestionSetResponse(
                row.id(), row.title(), row.sourceType(), row.sourceNote(), row.companyName(),
                row.targetRole(), row.companyIconKey(), row.sourceDocumentId(), texts.size(),
                row.archived(), row.archivedAt(),
                row.createdAt(), row.updatedAt(), indexed);
    }
}
