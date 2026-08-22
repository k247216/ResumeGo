package com.resumego.knowledge;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class KnowledgeRepository {

    private static final String STATUS_PENDING = "PENDING";
    private static final String JOB_COMPLETED = "COMPLETED";
    private static final String JOB_FAILED = "FAILED";

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<KnowledgeDocument> mapper = (rs, rowNum) -> new KnowledgeDocument(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("title"),
            rs.getString("source_type"),
            rs.getString("processing_status"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private final RowMapper<KnowledgeSourceFile> sourceMapper = (rs, rowNum) -> new KnowledgeSourceFile(
            rs.getLong("id"),
            rs.getLong("document_id"),
            rs.getLong("user_id"),
            rs.getString("original_name"),
            rs.getString("stored_relative_path"),
            rs.getString("mime_type"),
            rs.getString("extension"),
            rs.getLong("size_bytes"),
            rs.getString("sha256"),
            rs.getString("availability"),
            rs.getString("staging_relative_path"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private final RowMapper<KnowledgeImportJob> jobMapper = (rs, rowNum) -> new KnowledgeImportJob(
            rs.getLong("id"),
            rs.getLong("document_id"),
            rs.getLong("user_id"),
            rs.getLong("source_file_id"),
            rs.getString("job_status"),
            rs.getString("error_code"),
            rs.getTimestamp("started_at") != null ? rs.getTimestamp("started_at").toLocalDateTime() : null,
            rs.getTimestamp("finished_at") != null ? rs.getTimestamp("finished_at").toLocalDateTime() : null,
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private final RowMapper<KnowledgeExtractedContent> contentMapper = (rs, rowNum) -> new KnowledgeExtractedContent(
            rs.getLong("id"),
            rs.getLong("document_id"),
            rs.getLong("user_id"),
            rs.getString("content"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    public long insertDocument(long userId, String title, String sourceType, String processingStatus) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO knowledge_documents (user_id, title, source_type, processing_status)
                    VALUES (?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, userId);
            statement.setString(2, title);
            statement.setString(3, sourceType);
            statement.setString(4, processingStatus);
            return statement;
        }, keys);
        return requiredKey(keys, "创建知识文档失败：未返回主键");
    }

    public List<KnowledgeDocument> listByUser(long userId) {
        return jdbcTemplate.query("""
                SELECT id, user_id, title, source_type, processing_status, created_at, updated_at
                FROM knowledge_documents
                WHERE user_id = ?
                ORDER BY updated_at DESC, id DESC
                """, mapper, userId);
    }

    public Optional<KnowledgeDocument> findById(long userId, long id) {
        return jdbcTemplate.query("""
                SELECT id, user_id, title, source_type, processing_status, created_at, updated_at
                FROM knowledge_documents
                WHERE id = ? AND user_id = ?
                """, mapper, id, userId).stream().findFirst();
    }

    public Optional<KnowledgeSourceFile> findSourceFileBySha(long userId, String sha256) {
        return jdbcTemplate.query("""
                SELECT id, document_id, user_id, original_name, stored_relative_path, mime_type, staging_relative_path,
                       extension, size_bytes, sha256, availability, created_at, updated_at
                FROM knowledge_source_files
                WHERE user_id = ? AND sha256 = ?
                """, sourceMapper, userId, sha256).stream().findFirst();
    }

    public Optional<KnowledgeExtractedContent> findExtractedContentByDocument(long userId, long documentId) {
        return jdbcTemplate.query("""
                SELECT id, document_id, user_id, content, created_at, updated_at
                FROM knowledge_extracted_contents
                WHERE user_id = ? AND document_id = ?
                """, contentMapper, userId, documentId).stream().findFirst();
    }

    /**
     * 一次性创建 FILE document、source 与 import job（同一事务）。
     * 同一用户相同 sha256 并发竞争时由唯一约束 (user_id, sha256) 抛 DuplicateKeyException，
     * 事务回滚，不会留下孤立 document。
     */
    @Transactional
    public KnowledgeImportIds insertImportRecords(long userId, String title, KnowledgeSourceFileDraft draft) {
        long documentId = insertDocument(userId, title, "FILE", STATUS_PENDING);
        long sourceFileId = insertSourceFile(documentId, userId, draft);
        long importJobId = insertImportJob(documentId, userId, sourceFileId, STATUS_PENDING);
        return new KnowledgeImportIds(documentId, sourceFileId, importJobId);
    }

    private long insertSourceFile(long documentId, long userId, KnowledgeSourceFileDraft draft) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO knowledge_source_files
                        (document_id, user_id, original_name, stored_relative_path, mime_type,
                         staging_relative_path, extension, size_bytes, sha256, availability)
                    VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, documentId);
            statement.setLong(2, userId);
            statement.setString(3, draft.originalName());
            statement.setString(4, draft.storedRelativePath());
            statement.setString(5, draft.stagingRelativePath());
            statement.setString(6, draft.extension());
            statement.setLong(7, draft.sizeBytes());
            statement.setString(8, draft.sha256());
            statement.setString(9, draft.availability());
            return statement;
        }, keys);
        return requiredKey(keys, "创建知识来源文件失败：未返回主键");
    }

    private long insertImportJob(long documentId, long userId, long sourceFileId, String jobStatus) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO knowledge_import_jobs (document_id, user_id, source_file_id, job_status)
                    VALUES (?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, documentId);
            statement.setLong(2, userId);
            statement.setLong(3, sourceFileId);
            statement.setString(4, jobStatus);
            return statement;
        }, keys);
        return requiredKey(keys, "创建导入任务失败：未返回主键");
    }

    public void updateImportJobStatus(long importJobId, String jobStatus, String errorCode) {
        jdbcTemplate.update("""
                UPDATE knowledge_import_jobs
                SET job_status = ?,
                    error_code = ?,
                    started_at = CASE WHEN ? = 'RUNNING' AND started_at IS NULL THEN CURRENT_TIMESTAMP ELSE started_at END,
                    finished_at = CASE WHEN ? IN ('COMPLETED','FAILED') THEN CURRENT_TIMESTAMP ELSE NULL END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, jobStatus, errorCode, jobStatus, jobStatus, importJobId);
    }

    /** 提取成功：source 落位、保存正文、文档与任务 COMPLETED，同一事务。 */
    @Transactional
    public void completeImport(long documentId, long sourceFileId, long importJobId, long userId, String content) {
        updateSourceAvailability(sourceFileId, "AVAILABLE");
        insertExtractedContent(documentId, userId, content);
        updateDocumentStatus(documentId, "COMPLETED");
        updateImportJobStatus(importJobId, JOB_COMPLETED, null);
    }

    /** 提取失败：文档与任务 FAILED；副本已落位则 source 保持 AVAILABLE，否则保留 STAGED。 */
    @Transactional
    public void failImport(long documentId, long sourceFileId, long importJobId, String errorCode, boolean copyAvailable) {
        if (copyAvailable) {
            updateSourceAvailability(sourceFileId, "AVAILABLE");
        }
        updateDocumentStatus(documentId, "FAILED");
        updateImportJobStatus(importJobId, JOB_FAILED, errorCode);
    }

    private void insertExtractedContent(long documentId, long userId, String content) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO knowledge_extracted_contents (document_id, user_id, content)
                    VALUES (?, ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, documentId);
            statement.setLong(2, userId);
            statement.setString(3, content);
            return statement;
        }, keys);
        requiredKey(keys, "保存提取文本失败：未返回主键");
    }

    void updateSourceAvailability(long sourceFileId, String availability) {
        jdbcTemplate.update("""
                UPDATE knowledge_source_files
                SET availability = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, availability, sourceFileId);
    }

    private void updateDocumentStatus(long documentId, String processingStatus) {
        jdbcTemplate.update("""
                UPDATE knowledge_documents
                SET processing_status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, processingStatus, documentId);
    }


    private final RowMapper<KnowledgeCategory> categoryMapper = (rs, rowNum) -> new KnowledgeCategory(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("name"),
            rs.getString("normalized_name"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private final RowMapper<KnowledgeTag> tagMapper = (rs, rowNum) -> new KnowledgeTag(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("name"),
            rs.getString("normalized_name"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private final RowMapper<KnowledgeSearchRow> searchMapper = (rs, rowNum) -> new KnowledgeSearchRow(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("source_type"),
            rs.getString("processing_status"),
            rs.getTimestamp("created_at").toLocalDateTime().toString(),
            rs.getTimestamp("updated_at").toLocalDateTime().toString(),
            rs.getString("matched_field"),
            rs.getString("content")
    );

    // ---- categories ----

    public Optional<KnowledgeCategory> findCategoryByNormalizedName(long userId, String normalizedName) {
        return jdbcTemplate.query("""
                SELECT id, user_id, name, normalized_name, created_at, updated_at
                FROM knowledge_categories
                WHERE user_id = ? AND normalized_name = ?
                """, categoryMapper, userId, normalizedName).stream().findFirst();
    }

    public Optional<KnowledgeCategory> findCategoryById(long userId, long categoryId) {
        return jdbcTemplate.query("""
                SELECT id, user_id, name, normalized_name, created_at, updated_at
                FROM knowledge_categories
                WHERE id = ? AND user_id = ?
                """, categoryMapper, categoryId, userId).stream().findFirst();
    }

    public List<KnowledgeCategory> listCategories(long userId) {
        return jdbcTemplate.query("""
                SELECT id, user_id, name, normalized_name, created_at, updated_at
                FROM knowledge_categories
                WHERE user_id = ?
                ORDER BY updated_at DESC, id DESC
                """, categoryMapper, userId);
    }

    public long insertCategory(long userId, String name, String normalizedName) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO knowledge_categories (user_id, name, normalized_name)
                    VALUES (?, ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, userId);
            statement.setString(2, name);
            statement.setString(3, normalizedName);
            return statement;
        }, keys);
        return requiredKey(keys, "创建分类失败：未返回主键");
    }

    // ---- tags ----

    public Optional<KnowledgeTag> findTagByNormalizedName(long userId, String normalizedName) {
        return jdbcTemplate.query("""
                SELECT id, user_id, name, normalized_name, created_at, updated_at
                FROM knowledge_tags
                WHERE user_id = ? AND normalized_name = ?
                """, tagMapper, userId, normalizedName).stream().findFirst();
    }

    public Optional<KnowledgeTag> findTagById(long userId, long tagId) {
        return jdbcTemplate.query("""
                SELECT id, user_id, name, normalized_name, created_at, updated_at
                FROM knowledge_tags
                WHERE id = ? AND user_id = ?
                """, tagMapper, tagId, userId).stream().findFirst();
    }

    public List<KnowledgeTag> listTags(long userId) {
        return jdbcTemplate.query("""
                SELECT id, user_id, name, normalized_name, created_at, updated_at
                FROM knowledge_tags
                WHERE user_id = ?
                ORDER BY updated_at DESC, id DESC
                """, tagMapper, userId);
    }

    public long insertTag(long userId, String name, String normalizedName) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO knowledge_tags (user_id, name, normalized_name)
                    VALUES (?, ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, userId);
            statement.setString(2, name);
            statement.setString(3, normalizedName);
            return statement;
        }, keys);
        return requiredKey(keys, "创建标签失败：未返回主键");
    }

    // ---- document relations ----

    /** 每文档最多一个分类：先清后插，同一事务；重复关联结果一致（幂等）。 */
    @Transactional
    public void setDocumentCategory(long userId, long documentId, long categoryId) {
        jdbcTemplate.update("""
                DELETE FROM knowledge_document_categories
                WHERE document_id = ? AND user_id = ?
                """, documentId, userId);
        jdbcTemplate.update("""
                INSERT INTO knowledge_document_categories (document_id, category_id, user_id)
                VALUES (?, ?, ?)
                """, documentId, categoryId, userId);
    }

    @Transactional
    public void removeDocumentCategory(long userId, long documentId, long categoryId) {
        jdbcTemplate.update("""
                DELETE FROM knowledge_document_categories
                WHERE document_id = ? AND category_id = ? AND user_id = ?
                """, documentId, categoryId, userId);
    }

    /** 多对多，重复关联幂等（唯一约束兜底）。 */
    @Transactional
    public void addDocumentTag(long userId, long documentId, long tagId) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO knowledge_document_tags (document_id, tag_id, user_id)
                    VALUES (?, ?, ?)
                    """, documentId, tagId, userId);
        } catch (DuplicateKeyException ignored) {
            // 重复关联幂等
        }
    }

    @Transactional
    public void removeDocumentTag(long userId, long documentId, long tagId) {
        jdbcTemplate.update("""
                DELETE FROM knowledge_document_tags
                WHERE document_id = ? AND tag_id = ? AND user_id = ?
                """, documentId, tagId, userId);
    }


    // ---- document classification read ----

    /** 读取文档当前分类：按 document_id + user_id 隔离。 */
    public Optional<KnowledgeCategory> findDocumentCategory(long userId, long documentId) {
        return jdbcTemplate.query("""
                SELECT c.id, c.user_id, c.name, c.normalized_name, c.created_at, c.updated_at
                FROM knowledge_document_categories dc
                JOIN knowledge_categories c ON c.id = dc.category_id
                WHERE dc.document_id = ? AND dc.user_id = ?
                """, categoryMapper, documentId, userId).stream().findFirst();
    }

    /** 读取文档标签：冻结排序 name ASC, id ASC；按 document_id + user_id 隔离。 */
    public List<KnowledgeTag> listDocumentTags(long userId, long documentId) {
        return jdbcTemplate.query("""
                SELECT t.id, t.user_id, t.name, t.normalized_name, t.created_at, t.updated_at
                FROM knowledge_document_tags dt
                JOIN knowledge_tags t ON t.id = dt.tag_id
                WHERE dt.document_id = ? AND dt.user_id = ?
                ORDER BY t.name ASC, t.id ASC
                """, tagMapper, documentId, userId);
    }

    // ---- keyword search ----

    /**
     * 标题对所有文档命中；正文仅当 processing_status=COMPLETED 才参与匹配（LEFT JOIN 已限定）。
     * pattern 为 %...% 且已转义 wildcard；LOWER() 实现中英文大小写不敏感。
     * 可选 filter 必须属于当前用户（EXISTS 内带 user_id）。
     */
    public List<KnowledgeSearchRow> search(long userId, String pattern, Long categoryId, Long tagId) {
        StringBuilder sql = new StringBuilder("""
                SELECT d.id, d.title, d.source_type, d.processing_status,
                       d.created_at, d.updated_at,
                       CASE WHEN LOWER(d.title) LIKE LOWER(?) ESCAPE '!' THEN 'TITLE' ELSE 'CONTENT' END AS matched_field,
                       c.content
                FROM knowledge_documents d
                LEFT JOIN knowledge_extracted_contents c
                       ON c.document_id = d.id AND c.user_id = d.user_id
                      AND d.processing_status = 'COMPLETED'
                WHERE d.user_id = ?
                  AND (LOWER(d.title) LIKE LOWER(?) ESCAPE '!'
                       OR (d.processing_status = 'COMPLETED' AND c.content IS NOT NULL
                           AND LOWER(c.content) LIKE LOWER(?) ESCAPE '!'))
                """);
        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add(pattern);
        params.add(userId);
        params.add(pattern);
        params.add(pattern);
        if (categoryId != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM knowledge_document_categories dc WHERE dc.document_id = d.id AND dc.user_id = d.user_id AND dc.category_id = ?)");
            params.add(categoryId);
        }
        if (tagId != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM knowledge_document_tags dt WHERE dt.document_id = d.id AND dt.user_id = d.user_id AND dt.tag_id = ?)");
            params.add(tagId);
        }
        sql.append("ORDER BY d.updated_at DESC, d.id DESC\nLIMIT 100");
        return jdbcTemplate.query(sql.toString(), searchMapper, params.toArray());
    }


    private final RowMapper<KnowledgeDeleteConfirmation> confirmationMapper = (rs, rowNum) -> new KnowledgeDeleteConfirmation(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getLong("document_id"),
            rs.getString("token_hash"),
            rs.getTimestamp("expires_at").toLocalDateTime(),
            rs.getTimestamp("consumed_at") != null ? rs.getTimestamp("consumed_at").toLocalDateTime() : null,
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private final RowMapper<KnowledgeCleanupJob> cleanupMapper = (rs, rowNum) -> new KnowledgeCleanupJob(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getLong("document_id"),
            rs.getString("source_relative_path"),
            rs.getString("job_status"),
            rs.getString("error_code"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("started_at") != null ? rs.getTimestamp("started_at").toLocalDateTime() : null,
            rs.getTimestamp("finished_at") != null ? rs.getTimestamp("finished_at").toLocalDateTime() : null
    );

    public Optional<KnowledgeSourceFile> findSourceFileByDocument(long userId, long documentId) {
        return jdbcTemplate.query("""
                SELECT id, document_id, user_id, original_name, stored_relative_path, mime_type, staging_relative_path,
                       extension, size_bytes, sha256, availability, created_at, updated_at
                FROM knowledge_source_files
                WHERE document_id = ? AND user_id = ?
                """, sourceMapper, documentId, userId).stream().findFirst();
    }

    public Optional<KnowledgeImportJob> findImportJobByDocument(long userId, long documentId) {
        return jdbcTemplate.query("""
                SELECT id, document_id, user_id, source_file_id, job_status, error_code,
                       started_at, finished_at, created_at, updated_at
                FROM knowledge_import_jobs
                WHERE document_id = ? AND user_id = ?
                """, jobMapper, documentId, userId).stream().findFirst();
    }

    /** 记录 staging 相对路径（落位前）；成功后清空。 */
    public void updateSourceStagingPath(long sourceFileId, String stagingRelativePath) {
        jdbcTemplate.update("""
                UPDATE knowledge_source_files
                SET staging_relative_path = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, stagingRelativePath, sourceFileId);
    }

    /** 条件更新 FAILED -> RUNNING；返回是否 claim 成功（并发第二次返回 false）。 */
    public boolean claimImportJobForRetry(long userId, long documentId) {
        int updated = jdbcTemplate.update("""
                UPDATE knowledge_import_jobs
                SET job_status = 'RUNNING', started_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE document_id = ? AND user_id = ? AND job_status = 'FAILED'
                """, documentId, userId);
        return updated > 0;
    }

    /** 重试成功：upsert extracted content、source AVAILABLE、文档与 job COMPLETED（同一事务）。 */
    @Transactional
    public void completeImportRetry(long documentId, long sourceFileId, long importJobId, long userId, String content) {
        updateSourceAvailability(sourceFileId, "AVAILABLE");
        jdbcTemplate.update("""
                DELETE FROM knowledge_extracted_contents WHERE document_id = ? AND user_id = ?
                """, documentId, userId);
        insertExtractedContent(documentId, userId, content);
        updateDocumentStatus(documentId, "COMPLETED");
        updateImportJobStatus(importJobId, JOB_COMPLETED, null);
    }

    // ---- deletion ----

    /** 生成/替换文档的待确认删除 token（每文档一个，新 token 覆盖旧 token）。 */
    public void replaceDeletionConfirmation(long userId, long documentId, String tokenHash, java.time.LocalDateTime expiresAt) {
        jdbcTemplate.update("""
                DELETE FROM knowledge_delete_confirmations WHERE document_id = ? AND user_id = ?
                """, documentId, userId);
        jdbcTemplate.update("""
                INSERT INTO knowledge_delete_confirmations (user_id, document_id, token_hash, expires_at)
                VALUES (?, ?, ?, ?)
                """, userId, documentId, tokenHash, java.sql.Timestamp.valueOf(expiresAt));
    }

    public Optional<KnowledgeDeleteConfirmation> findDeletionConfirmation(long userId, long documentId) {
        return jdbcTemplate.query("""
                SELECT id, user_id, document_id, token_hash, expires_at, consumed_at, created_at
                FROM knowledge_delete_confirmations
                WHERE document_id = ? AND user_id = ?
                """, confirmationMapper, documentId, userId).stream().findFirst();
    }

    public void consumeDeletionConfirmation(long confirmationId) {
        jdbcTemplate.update("""
                UPDATE knowledge_delete_confirmations
                SET consumed_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, confirmationId);
    }

    public long insertCleanupJob(long userId, long documentId, String sourceRelativePath, String jobStatus) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO knowledge_cleanup_jobs
                        (user_id, document_id, source_relative_path, job_status)
                    VALUES (?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, userId);
            statement.setLong(2, documentId);
            statement.setString(3, sourceRelativePath);
            statement.setString(4, jobStatus);
            return statement;
        }, keys);
        return requiredKey(keys, "创建清理任务失败：未返回主键");
    }

    public Optional<KnowledgeCleanupJob> findCleanupJobById(long userId, long cleanupJobId) {
        return jdbcTemplate.query("""
                SELECT id, user_id, document_id, source_relative_path,
                       job_status, error_code, created_at, started_at, finished_at
                FROM knowledge_cleanup_jobs
                WHERE id = ? AND user_id = ?
                """, cleanupMapper, cleanupJobId, userId).stream().findFirst();
    }

    public List<KnowledgeCleanupJob> listCleanupJobsByStatus(long userId, String jobStatus) {
        return jdbcTemplate.query("""
                SELECT id, user_id, document_id, source_relative_path,
                       job_status, error_code, created_at, started_at, finished_at
                FROM knowledge_cleanup_jobs
                WHERE user_id = ? AND job_status = ?
                ORDER BY id ASC
                """, cleanupMapper, userId, jobStatus);
    }

    /** 条件更新 cleanup job 状态；返回是否成功。 */
    public boolean claimCleanupJob(long userId, long cleanupJobId, String fromStatus, String toStatus) {
        int updated = jdbcTemplate.update("""
                UPDATE knowledge_cleanup_jobs
                SET job_status = ?, started_at = CASE WHEN ? = 'RUNNING' AND started_at IS NULL
                        THEN CURRENT_TIMESTAMP ELSE started_at END, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ? AND job_status = ?
                """, toStatus, toStatus, cleanupJobId, userId, fromStatus);
        return updated > 0;
    }

    public void updateCleanupJobStatus(long cleanupJobId, String jobStatus, String errorCode) {
        jdbcTemplate.update("""
                UPDATE knowledge_cleanup_jobs
                SET job_status = ?,
                    error_code = ?,
                    started_at = CASE WHEN ? = 'RUNNING' AND started_at IS NULL THEN CURRENT_TIMESTAMP ELSE started_at END,
                    finished_at = CASE WHEN ? IN ('COMPLETED','FAILED') THEN CURRENT_TIMESTAMP ELSE NULL END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, jobStatus, errorCode, jobStatus, jobStatus, cleanupJobId);
    }

    /** COMPLETED 时清空受管路径：彻底清理不保留个人路径。 */
    public void completeCleanupJob(long cleanupJobId) {
        jdbcTemplate.update("""
                UPDATE knowledge_cleanup_jobs
                SET job_status = 'COMPLETED',
                    error_code = NULL,
                    source_relative_path = NULL,
                    finished_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, cleanupJobId);
    }

    /** 启动恢复：document 仍 FAILED 的 RUNNING import job（进程崩溃残留）。 */
    public List<KnowledgeImportJob> listStuckRunningImportJobs(long userId) {
        return jdbcTemplate.query("""
                SELECT ij.id, ij.document_id, ij.user_id, ij.source_file_id, ij.job_status, ij.error_code,
                       ij.started_at, ij.finished_at, ij.created_at, ij.updated_at
                FROM knowledge_import_jobs ij
                WHERE ij.user_id = ? AND ij.job_status = 'RUNNING'
                  AND EXISTS (SELECT 1 FROM knowledge_documents d
                              WHERE d.id = ij.document_id AND d.processing_status = 'FAILED')
                """, jobMapper, userId);
    }

    /** 把残留 RUNNING import job 重置回 FAILED，保留原 errorCode；并发安全（条件更新）。 */
    public boolean resetImportJobToFailed(long importJobId) {
        int updated = jdbcTemplate.update("""
                UPDATE knowledge_import_jobs
                SET job_status = 'FAILED', updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND job_status = 'RUNNING'
                """, importJobId);
        return updated > 0;
    }

    /** 删除文档：FK cascade 清理 content/import job/category/tag relation/source metadata。 */
    public void deleteDocumentById(long userId, long documentId) {
        jdbcTemplate.update("""
                DELETE FROM knowledge_documents WHERE id = ? AND user_id = ?
                """, documentId, userId);
    }

    private long requiredKey(KeyHolder keys, String message) {
        Number key = keys.getKey();
        if (key == null) {
            throw new IllegalStateException(message);
        }
        return key.longValue();
    }
}
