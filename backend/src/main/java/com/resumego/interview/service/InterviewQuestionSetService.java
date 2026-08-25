package com.resumego.interview.service;

import com.resumego.common.CurrentUser;
import com.resumego.interview.QuestionSourceType;
import com.resumego.interview.dto.InterviewQuestionSetRequest;
import com.resumego.interview.dto.InterviewQuestionSetResponse;
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

    private final InterviewQuestionSetRepository repository;

    public InterviewQuestionSetService(InterviewQuestionSetRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public InterviewQuestionSetResponse create(InterviewQuestionSetRequest request) {
        String title = validateTitle(request);
        QuestionSourceType sourceType = validateSourceType(request);
        String sourceNote = validateSourceNote(request);
        List<String> questions = validateQuestions(request);

        long setId = repository.createSet(CurrentUser.DEMO_USER_ID, title, sourceType, sourceNote, questions);
        return toDetailResponse(repository.findSetById(CurrentUser.DEMO_USER_ID, setId), setId);
    }

    /** 列表：不含题目正文。 */
    public List<InterviewQuestionSetResponse> list() {
        return repository.findAllSets(CurrentUser.DEMO_USER_ID).stream()
                .map(this::toMetaResponse)
                .toList();
    }

    public InterviewQuestionSetResponse get(long setId) {
        InterviewQuestionSetRepository.QuestionSetRow row = requireOwnedSet(setId);
        return toDetailResponse(row, setId);
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
        List<String> questions = validateQuestions(request);

        repository.replaceSet(CurrentUser.DEMO_USER_ID, setId, title, sourceType, sourceNote, questions);
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

    private InterviewQuestionSetResponse toMetaResponse(InterviewQuestionSetRepository.QuestionSetRow row) {
        return new InterviewQuestionSetResponse(
                row.id(), row.title(), row.sourceType(), row.sourceNote(),
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
                row.id(), row.title(), row.sourceType(), row.sourceNote(),
                row.archived(), row.archivedAt(),
                row.createdAt(), row.updatedAt(), indexed);
    }
}
