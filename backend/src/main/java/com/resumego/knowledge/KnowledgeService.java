package com.resumego.knowledge;

import com.resumego.common.CurrentUser;
import com.resumego.knowledge.dto.CreateKnowledgeDocumentRequest;
import com.resumego.knowledge.dto.KnowledgeContentResponse;
import com.resumego.knowledge.dto.KnowledgeDocumentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class KnowledgeService {

    private static final String SOURCE_NOTE = "NOTE";
    private static final String STATUS_NOT_STARTED = "NOT_STARTED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final long MAX_NOTE_CONTENT_BYTES = 1024L * 1024;

    private final KnowledgeRepository repository;

    public KnowledgeService(KnowledgeRepository repository) {
        this.repository = repository;
    }

    private String normalizeTitle(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        String collapsed = trimmed.replaceAll("\\s+", " ");
        if (collapsed.isEmpty()) throw new IllegalArgumentException("标题不能为空");
        if (collapsed.length() > 120) throw new IllegalArgumentException("标题不能超过 120 字符");
        return collapsed;
    }

    @Transactional
    public KnowledgeDocumentResponse create(CreateKnowledgeDocumentRequest request) {
        if (request == null) throw new IllegalArgumentException("请求不能为空");
        String title = normalizeTitle(request.title());
        if (!SOURCE_NOTE.equals(request.sourceType())) {
            throw new IllegalArgumentException("本阶段仅支持创建 NOTE 类型知识文档，不支持 FILE");
        }
        long id = repository.insertDocument(userId(), title, SOURCE_NOTE, STATUS_NOT_STARTED);
        return findResponse(id);
    }

    public List<KnowledgeDocumentResponse> list() {
        return repository.listByUser(userId()).stream().map(this::toResponse).toList();
    }

    public KnowledgeDocumentResponse get(long documentId) {
        return findResponse(documentId);
    }

    /** 保存 NOTE 正文：原样 UTF-8（允许空串，≤1 MiB），同事务置 COMPLETED；FILE/缺失拒绝；不记录正文。 */
    public KnowledgeContentResponse saveNoteContent(long documentId, String content) {
        KnowledgeDocument doc = repository.findById(userId(), documentId)
                .orElseThrow(() -> new NoSuchElementException("知识文档不存在"));
        if (!SOURCE_NOTE.equals(doc.sourceType())) {
            throw new IllegalStateException("仅 NOTE 文档支持保存正文");
        }
        if (content == null) {
            throw new IllegalArgumentException("正文不能为空值");
        }
        if (content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_NOTE_CONTENT_BYTES) {
            throw new IllegalArgumentException("正文不能超过 1 MiB");
        }
        repository.saveNoteContent(documentId, userId(), content);
        return new KnowledgeContentResponse(documentId, content);
    }

    /** 返回当前用户的提取文本；未完成 409，缺失/他人 404。 */
    public KnowledgeContentResponse getContent(long documentId) {
        KnowledgeDocument doc = repository.findById(userId(), documentId)
                .orElseThrow(() -> new NoSuchElementException("知识文档不存在"));
        if (!STATUS_COMPLETED.equals(doc.processingStatus())) {
            throw new IllegalStateException("知识文档尚未完成文本提取");
        }
        KnowledgeExtractedContent content = repository.findExtractedContentByDocument(userId(), documentId)
                .orElseThrow(() -> new IllegalStateException("提取内容不可用"));
        return new KnowledgeContentResponse(doc.id(), content.content());
    }

    private KnowledgeDocumentResponse findResponse(long id) {
        KnowledgeDocument doc = repository.findById(userId(), id)
                .orElseThrow(() -> new NoSuchElementException("知识文档不存在"));
        return toResponse(doc);
    }

    private KnowledgeDocumentResponse toResponse(KnowledgeDocument doc) {
        return new KnowledgeDocumentResponse(
                doc.id(),
                doc.title(),
                doc.sourceType(),
                doc.processingStatus(),
                null,
                doc.createdAt().toString(),
                doc.updatedAt().toString()
        );
    }

    private long userId() {
        return CurrentUser.DEMO_USER_ID;
    }
}
