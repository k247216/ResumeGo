package com.resumego.knowledge;

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
                SELECT id, document_id, user_id, original_name, stored_relative_path, mime_type,
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
                         extension, size_bytes, sha256, availability)
                    VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, documentId);
            statement.setLong(2, userId);
            statement.setString(3, draft.originalName());
            statement.setString(4, draft.storedRelativePath());
            statement.setString(5, draft.extension());
            statement.setLong(6, draft.sizeBytes());
            statement.setString(7, draft.sha256());
            statement.setString(8, draft.availability());
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

    private void updateSourceAvailability(long sourceFileId, String availability) {
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

    private long requiredKey(KeyHolder keys, String message) {
        Number key = keys.getKey();
        if (key == null) {
            throw new IllegalStateException(message);
        }
        return key.longValue();
    }
}
