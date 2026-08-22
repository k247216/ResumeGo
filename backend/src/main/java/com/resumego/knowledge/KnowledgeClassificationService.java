package com.resumego.knowledge;

import com.resumego.common.CurrentUser;
import com.resumego.knowledge.dto.CreateKnowledgeNameRequest;
import com.resumego.knowledge.dto.KnowledgeDocumentClassificationResponse;
import com.resumego.knowledge.dto.KnowledgeCategoryResponse;
import com.resumego.knowledge.dto.KnowledgeDocumentResponse;
import com.resumego.knowledge.dto.KnowledgeSearchItemResponse;
import com.resumego.knowledge.dto.KnowledgeTagResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * 分类、标签与关键词搜索。
 * 所有权贯穿所有读写：查询与关联都带 user_id；外部用户/不存在资源一律 404。
 */
@Service
public class KnowledgeClassificationService {

    private static final int MAX_NAME_LENGTH = 40;
    private static final int MAX_QUERY_LENGTH = 100;
    private static final int MAX_RESULTS = 100;
    private static final int SNIPPET_LIMIT = 240;
    private static final int SNIPPET_BEFORE = 100;
    private static final int SNIPPET_AFTER = 140;

    private final KnowledgeRepository repository;

    public KnowledgeClassificationService(KnowledgeRepository repository) {
        this.repository = repository;
    }

    // ---- names ----

    /** trim + 合并内部空白 + 1..40；规范化名用 Locale.ROOT 小写，不跨用户合并。 */
    private String normalizeDisplayName(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("名称不能为空");
        }
        String collapsed = raw.trim().replaceAll("\\s+", " ");
        if (collapsed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("名称不能超过 " + MAX_NAME_LENGTH + " 字符");
        }
        return collapsed;
    }

    private String normalizeKey(String displayName) {
        return displayName.toLowerCase(Locale.ROOT);
    }

    // ---- categories ----

    public List<KnowledgeCategoryResponse> listCategories() {
        return repository.listCategories(userId()).stream().map(this::toCategoryResponse).toList();
    }

    public KnowledgeNameCreateResult<KnowledgeCategoryResponse> createCategory(CreateKnowledgeNameRequest request) {
        return createName(request, true);
    }

    // ---- tags ----

    public List<KnowledgeTagResponse> listTags() {
        return repository.listTags(userId()).stream().map(this::toTagResponse).toList();
    }

    public KnowledgeNameCreateResult<KnowledgeTagResponse> createTag(CreateKnowledgeNameRequest request) {
        return createName(request, false);
    }

    private <T> KnowledgeNameCreateResult<T> createName(CreateKnowledgeNameRequest request, boolean category) {
        if (request == null || request.name() == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        String display = normalizeDisplayName(request.name());
        String key = normalizeKey(display);
        if (category) {
            KnowledgeCategory existing = repository.findCategoryByNormalizedName(userId(), key).orElse(null);
            if (existing != null) {
                return new KnowledgeNameCreateResult<>(cast(toCategoryResponse(existing)), false);
            }
            try {
                long id = repository.insertCategory(userId(), display, key);
                KnowledgeCategory created = repository.findCategoryById(userId(), id).orElseThrow();
                return new KnowledgeNameCreateResult<>(cast(toCategoryResponse(created)), true);
            } catch (DuplicateKeyException exception) {
                KnowledgeCategory raced = repository.findCategoryByNormalizedName(userId(), key).orElseThrow();
                return new KnowledgeNameCreateResult<>(cast(toCategoryResponse(raced)), false);
            }
        }
        KnowledgeTag existing = repository.findTagByNormalizedName(userId(), key).orElse(null);
        if (existing != null) {
            return new KnowledgeNameCreateResult<>(cast(toTagResponse(existing)), false);
        }
        try {
            long id = repository.insertTag(userId(), display, key);
            KnowledgeTag created = repository.findTagById(userId(), id).orElseThrow();
            return new KnowledgeNameCreateResult<>(cast(toTagResponse(created)), true);
        } catch (DuplicateKeyException exception) {
            KnowledgeTag raced = repository.findTagByNormalizedName(userId(), key).orElseThrow();
            return new KnowledgeNameCreateResult<>(cast(toTagResponse(raced)), false);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Object value) {
        return (T) value;
    }

    // ---- document relations ----

    /** 关联或解除不存在/外部用户的 document、category 一律 404，不泄露所有权。 */
    @Transactional
    public void setDocumentCategory(long documentId, long categoryId) {
        requireDocument(documentId);
        requireCategory(categoryId);
        repository.setDocumentCategory(userId(), documentId, categoryId);
    }

    @Transactional
    public void removeDocumentCategory(long documentId, long categoryId) {
        requireDocument(documentId);
        requireCategory(categoryId);
        repository.removeDocumentCategory(userId(), documentId, categoryId);
    }

    @Transactional
    public void addDocumentTag(long documentId, long tagId) {
        requireDocument(documentId);
        requireTag(tagId);
        repository.addDocumentTag(userId(), documentId, tagId);
    }

    @Transactional
    public void removeDocumentTag(long documentId, long tagId) {
        requireDocument(documentId);
        requireTag(tagId);
        repository.removeDocumentTag(userId(), documentId, tagId);
    }

    private void requireDocument(long documentId) {
        repository.findById(userId(), documentId)
                .orElseThrow(() -> new NoSuchElementException("知识文档不存在"));
    }

    private void requireCategory(long categoryId) {
        repository.findCategoryById(userId(), categoryId)
                .orElseThrow(() -> new NoSuchElementException("分类不存在"));
    }

    private void requireTag(long tagId) {
        repository.findTagById(userId(), tagId)
                .orElseThrow(() -> new NoSuchElementException("标签不存在"));
    }

    // ---- document classification read ----

    /** 读取文档现有关联：document 必须属于当前用户，否则 404；空关联诚实返回 null/[]。 */
    public KnowledgeDocumentClassificationResponse getDocumentClassification(long documentId) {
        requireDocument(documentId);
        KnowledgeCategory category = repository.findDocumentCategory(userId(), documentId).orElse(null);
        List<KnowledgeTag> tags = repository.listDocumentTags(userId(), documentId);
        return new KnowledgeDocumentClassificationResponse(
                category == null ? null : toCategoryResponse(category),
                tags.stream().map(this::toTagResponse).toList());
    }

    // ---- search ----

    /**
     * q trim 后 1..100；字面子串匹配（wildcard 已转义）；标题所有文档可命中，
     * 正文仅 COMPLETED；可选 filter 必须属于当前用户；updated_at DESC, id DESC，最多 100。
     */
    public List<KnowledgeSearchItemResponse> search(String q, Long categoryId, Long tagId) {
        String query = q == null ? "" : q.trim();
        if (query.isEmpty() || query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("搜索词长度需为 1-" + MAX_QUERY_LENGTH + " 个字符");
        }
        if (categoryId != null) {
            requireCategory(categoryId);
        }
        if (tagId != null) {
            requireTag(tagId);
        }
        String pattern = "%" + escapeLike(query) + "%";
        List<KnowledgeSearchRow> rows = repository.search(userId(), pattern, categoryId, tagId);
        return rows.stream()
                .limit(MAX_RESULTS)
                .map(row -> toSearchItem(row, query))
                .toList();
    }

    /**
     * 可移植单字符转义（ESCAPE '!'，MySQL 反斜杠字符串语义下安全）：
     * 先 ! -> !!，再 % -> !%，_ -> !_；反斜杠作为普通字面字符，不充当 escape char。
     */
    static String escapeLike(String input) {
        return input.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    private KnowledgeSearchItemResponse toSearchItem(KnowledgeSearchRow row, String query) {
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        KnowledgeDocumentResponse document = new KnowledgeDocumentResponse(
                row.documentId(), row.title(), row.sourceType(), row.processingStatus(), null,
                row.createdAt(), row.updatedAt());
        if ("CONTENT".equals(row.matchedField()) && row.content() != null) {
            int hit = indexOfIgnoreCase(row.content(), lowerQuery);
            if (hit >= 0) {
                return new KnowledgeSearchItemResponse(document, "CONTENT",
                        buildSnippet(row.content(), hit, query.length()), lineNumber(row.content(), hit));
            }
        }
        int hit = indexOfIgnoreCase(row.title(), lowerQuery);
        return new KnowledgeSearchItemResponse(document, "TITLE",
                buildSnippet(row.title(), Math.max(hit, 0), query.length()), null);
    }

    private int indexOfIgnoreCase(String text, String lowerQuery) {
        return text.toLowerCase(Locale.ROOT).indexOf(lowerQuery);
    }

    /** 命中位置为中心截取不超过 240 字片段，前后加省略号。 */
    static String buildSnippet(String text, int hitIndex, int queryLength) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int start = Math.max(0, hitIndex - SNIPPET_BEFORE);
        int end = Math.min(text.length(), hitIndex + Math.max(queryLength, 1) + SNIPPET_AFTER);
        String snippet = text.substring(start, end);
        boolean prefix = start > 0;
        boolean suffix = end < text.length();
        if (prefix) snippet = "…" + snippet;
        if (suffix) snippet = snippet + "…";
        if (snippet.length() > SNIPPET_LIMIT) {
            snippet = snippet.substring(0, SNIPPET_LIMIT);
        }
        return snippet;
    }

    /** 1-based 行号：命中位置之前的换行数 + 1。 */
    static int lineNumber(String text, int hitIndex) {
        int line = 1;
        int bound = Math.min(hitIndex, text.length());
        for (int i = 0; i < bound; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private KnowledgeCategoryResponse toCategoryResponse(KnowledgeCategory category) {
        return new KnowledgeCategoryResponse(category.id(), category.name(), category.normalizedName(),
                category.createdAt().toString(), category.updatedAt().toString());
    }

    private KnowledgeTagResponse toTagResponse(KnowledgeTag tag) {
        return new KnowledgeTagResponse(tag.id(), tag.name(), tag.normalizedName(),
                tag.createdAt().toString(), tag.updatedAt().toString());
    }

    private long userId() {
        return CurrentUser.DEMO_USER_ID;
    }
}
