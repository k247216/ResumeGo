package com.resumego.knowledge;

import com.resumego.common.CurrentUser;
import com.resumego.knowledge.dto.CreateKnowledgeCategoryRequest;
import com.resumego.knowledge.dto.CreateKnowledgeNameRequest;
import com.resumego.knowledge.dto.KnowledgeCategoryNodeResponse;
import com.resumego.knowledge.dto.KnowledgeCategoryResponse;
import com.resumego.knowledge.dto.KnowledgeDocumentClassificationResponse;
import com.resumego.knowledge.dto.UpdateKnowledgeCategoryRequest;
import com.resumego.knowledge.dto.KnowledgeDocumentResponse;
import com.resumego.knowledge.dto.KnowledgeSearchItemResponse;
import com.resumego.knowledge.dto.KnowledgeTagResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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
    /** 最大层级五级：根 depth=0，最多 depth 4。 */
    private static final int MAX_TREE_DEPTH = 4;

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

    // ---- categories (hierarchical library folders) ----

    /** 扁平节点列表：depth/count 由真实关联在内存计算；前端构树。 */
    public List<KnowledgeCategoryNodeResponse> listCategoryTree() {
        List<KnowledgeCategory> all = repository.listCategories(userId());
        Map<Long, Integer> directCounts = repository.listCategoryDocumentCounts(userId());
        Map<Long, List<Long>> children = childrenMap(all);
        Map<Long, Integer> depths = computeDepths(all, children);
        Map<Long, Integer> descendantCounts = computeDescendantCounts(all, children, directCounts, depths);
        return all.stream().map(category -> new KnowledgeCategoryNodeResponse(
                category.id(),
                category.name(),
                category.normalizedName(),
                category.parentId(),
                depths.getOrDefault(category.id(), 0),
                directCounts.getOrDefault(category.id(), 0),
                descendantCounts.getOrDefault(category.id(), 0),
                category.createdAt().toString(),
                category.updatedAt().toString())).toList();
    }

    /** 创建分类（文件夹）：parentId 可为 null（根）；跨用户 parent 404；层级不超过五级。 */
    public KnowledgeNameCreateResult<KnowledgeCategoryResponse> createCategory(CreateKnowledgeCategoryRequest request) {
        if (request == null || request.name() == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        String display = normalizeDisplayName(request.name());
        String key = normalizeKey(display);
        Long parentId = request.parentId();
        if (parentId != null) {
            requireCategory(parentId);
            if (depthOf(parentId) + 1 > MAX_TREE_DEPTH) {
                throw new IllegalArgumentException("分类层级不能超过五级");
            }
        }
        KnowledgeCategory existing = repository.findCategoryByNormalizedName(userId(), key).orElse(null);
        if (existing != null) {
            return new KnowledgeNameCreateResult<>(toCategoryResponse(existing), false);
        }
        try {
            long id = repository.insertCategoryWithParent(userId(), display, key, parentId);
            KnowledgeCategory created = repository.findCategoryById(userId(), id).orElseThrow();
            return new KnowledgeNameCreateResult<>(toCategoryResponse(created), true);
        } catch (DuplicateKeyException exception) {
            KnowledgeCategory raced = repository.findCategoryByNormalizedName(userId(), key).orElseThrow();
            return new KnowledgeNameCreateResult<>(toCategoryResponse(raced), false);
        }
    }

    /** 更新名称与父节点（移动）：parentId 显式提供，可为 null（移到根）；拒绝自身/后代循环与超五级。 */
    @Transactional
    public KnowledgeCategoryResponse updateCategory(long categoryId, UpdateKnowledgeCategoryRequest request) {
        if (request == null || request.name() == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        KnowledgeCategory current = repository.findCategoryById(userId(), categoryId)
                .orElseThrow(() -> new NoSuchElementException("分类不存在"));
        String display = normalizeDisplayName(request.name());
        String key = normalizeKey(display);
        Long newParentId = request.parentId();
        if (newParentId != null) {
            requireCategory(newParentId);
            if (newParentId == categoryId) {
                throw new IllegalArgumentException("分类不能作为自身的父级");
            }
            if (isAncestor(categoryId, newParentId)) {
                throw new IllegalArgumentException("不能移动到自身的后代分类下");
            }
            int parentDepth = depthOf(newParentId);
            int subtreeHeight = subtreeHeight(categoryId);
            if (parentDepth + 1 + subtreeHeight > MAX_TREE_DEPTH) {
                throw new IllegalArgumentException("移动后分类层级不能超过五级");
            }
        }
        try {
            repository.updateCategory(userId(), categoryId, display, key, newParentId);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("同名称分类已存在");
        }
        return toCategoryResponse(repository.findCategoryById(userId(), categoryId).orElseThrow());
    }

    /** 仅删除空叶节点：无子分类且无直属文档，否则 CATEGORY_NOT_EMPTY（409）；不得 cascade。 */
    @Transactional
    public void deleteCategory(long categoryId) {
        KnowledgeCategory category = repository.findCategoryById(userId(), categoryId)
                .orElseThrow(() -> new NoSuchElementException("分类不存在"));
        List<KnowledgeCategory> all = repository.listCategories(userId());
        boolean hasChildren = all.stream().anyMatch(c -> c.parentId() != null && c.parentId() == categoryId);
        int directDocuments = repository.listCategoryDocumentCounts(userId()).getOrDefault(categoryId, 0);
        if (hasChildren || directDocuments > 0) {
            throw new IllegalStateException("CATEGORY_NOT_EMPTY: 分类下仍有子分类或资料，不能删除");
        }
        repository.deleteCategoryById(userId(), categoryId);
    }

    // ---- tree helpers ----

    private Map<Long, List<Long>> childrenMap(List<KnowledgeCategory> all) {
        Map<Long, List<Long>> children = new java.util.HashMap<>();
        for (KnowledgeCategory category : all) {
            if (category.parentId() != null) {
                children.computeIfAbsent(category.parentId(), k -> new java.util.ArrayList<>()).add(category.id());
            }
        }
        return children;
    }

    private Map<Long, Integer> computeDepths(List<KnowledgeCategory> all, Map<Long, List<Long>> children) {
        Map<Long, Integer> depths = new java.util.HashMap<>();
        java.util.ArrayDeque<Long> queue = new java.util.ArrayDeque<>();
        for (KnowledgeCategory category : all) {
            if (category.parentId() == null) {
                depths.put(category.id(), 0);
                queue.add(category.id());
            }
        }
        while (!queue.isEmpty()) {
            long id = queue.poll();
            int depth = depths.get(id);
            for (long child : children.getOrDefault(id, List.of())) {
                depths.putIfAbsent(child, depth + 1);
                queue.add(child);
            }
        }
        // 防御（BFS 之后）：parent 缺失的孤儿按根处理
        for (KnowledgeCategory category : all) {
            if (!depths.containsKey(category.id())) {
                depths.put(category.id(), 0);
            }
        }
        return depths;
    }

    private Map<Long, Integer> computeDescendantCounts(List<KnowledgeCategory> all, Map<Long, List<Long>> children,
                                                       Map<Long, Integer> directCounts, Map<Long, Integer> depths) {
        Map<Long, Integer> counts = new java.util.HashMap<>();
        java.util.function.Function<Long, Integer> compute = new java.util.function.Function<>() {
            @Override
            public Integer apply(Long id) {
                Integer cached = counts.get(id);
                if (cached != null) return cached;
                int total = directCounts.getOrDefault(id, 0);
                for (long child : children.getOrDefault(id, List.of())) {
                    if (depths.getOrDefault(child, 0) > depths.getOrDefault(id, 0)) {
                        total += apply(child);
                    }
                }
                counts.put(id, total);
                return total;
            }
        };
        for (KnowledgeCategory category : all) {
            compute.apply(category.id());
        }
        return counts;
    }

    /** 分类所在深度（根=0），逐级上查。 */
    private int depthOf(long categoryId) {
        Map<Long, Long> parentById = new java.util.HashMap<>();
        for (KnowledgeCategory category : repository.listCategories(userId())) {
            if (category.parentId() != null) {
                parentById.put(category.id(), category.parentId());
            }
        }
        int depth = 0;
        Long cursor = parentById.get(categoryId);
        java.util.Set<Long> seen = new java.util.HashSet<>();
        while (cursor != null && seen.add(cursor)) {
            depth++;
            cursor = parentById.get(cursor);
        }
        return depth;
    }

    /** 以 categoryId 为根的子树相对高度（自身=0）。 */
    private int subtreeHeight(long categoryId) {
        Map<Long, Integer> depths = computeDepths(repository.listCategories(userId()), childrenMap(repository.listCategories(userId())));
        int rootDepth = depths.getOrDefault(categoryId, 0);
        int maxDepth = rootDepth;
        for (Map.Entry<Long, Integer> entry : depths.entrySet()) {
            if (isAncestor(categoryId, entry.getKey())) {
                maxDepth = Math.max(maxDepth, entry.getValue());
            }
        }
        return maxDepth - rootDepth;
    }

    /** nodeId 是否位于 ancestorId 的子树（沿 parent 链上溯检查）。 */
    private boolean isAncestor(long ancestorId, long nodeId) {
        Map<Long, Long> parentById = new java.util.HashMap<>();
        for (KnowledgeCategory category : repository.listCategories(userId())) {
            if (category.parentId() != null) {
                parentById.put(category.id(), category.parentId());
            }
        }
        Long cursor = nodeId;
        java.util.Set<Long> seen = new java.util.HashSet<>();
        while (cursor != null && seen.add(cursor)) {
            if (cursor == ancestorId) {
                return true;
            }
            cursor = parentById.get(cursor);
        }
        return false;
    }

    /** 子树（含自身）分类 id 集合，供 includeDescendants 搜索。 */
    private java.util.Set<Long> descendantIds(long categoryId) {
        List<KnowledgeCategory> all = repository.listCategories(userId());
        Map<Long, List<Long>> children = childrenMap(all);
        java.util.Set<Long> result = new java.util.LinkedHashSet<>();
        java.util.ArrayDeque<Long> queue = new java.util.ArrayDeque<>();
        queue.add(categoryId);
        while (!queue.isEmpty()) {
            long id = queue.poll();
            if (!result.add(id)) continue;
            queue.addAll(children.getOrDefault(id, List.of()));
        }
        return result;
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
    /** includeDescendants 仅在 categoryId 非空时生效：true 返回该分类及其全部后代分类的直属文档。 */
    public List<KnowledgeSearchItemResponse> search(String q, Long categoryId, Long tagId, boolean includeDescendants) {
        String query = q == null ? "" : q.trim();
        if (query.isEmpty() || query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("搜索词长度需为 1-" + MAX_QUERY_LENGTH + " 个字符");
        }
        java.util.Collection<Long> categoryIds = null;
        if (categoryId != null) {
            requireCategory(categoryId);
            categoryIds = includeDescendants ? descendantIds(categoryId) : List.of(categoryId);
        }
        if (tagId != null) {
            requireTag(tagId);
        }
        String pattern = "%" + escapeLike(query) + "%";
        List<KnowledgeSearchRow> rows = repository.search(userId(), pattern, categoryIds, tagId);
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
                row.documentId(), row.title(), row.sourceType(), row.processingStatus(), row.sourceFile(),
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
                category.parentId(), category.createdAt().toString(), category.updatedAt().toString());
    }

    private KnowledgeTagResponse toTagResponse(KnowledgeTag tag) {
        return new KnowledgeTagResponse(tag.id(), tag.name(), tag.normalizedName(),
                tag.createdAt().toString(), tag.updatedAt().toString());
    }

    private long userId() {
        return CurrentUser.DEMO_USER_ID;
    }
}
