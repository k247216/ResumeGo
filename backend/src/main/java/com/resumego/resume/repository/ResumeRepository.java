package com.resumego.resume.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.resume.dto.ResumeVersionDTO;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Repository
public class ResumeRepository {

    public record EvidenceRefDraft(Long evidenceId, String sectionKey) {
    }

    public record ProjectEvidenceDraft(
            String title,
            String situation,
            String actionText,
            String resultText,
            List<String> skillTags,
            String sourceNote
    ) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ResumeRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<Long> findIdsByUserId(long userId) {
        return findIdsByUserId(userId, null, false);
    }

    public List<Long> findIdsByUserId(long userId, String kind, boolean archived) {
        StringBuilder sql = new StringBuilder(
                "SELECT id FROM resumes WHERE user_id = ? AND deleted_at IS NULL AND archived_at IS ");
        sql.append(archived ? "NOT NULL" : "NULL");
        Object[] args = kind != null
                ? new Object[]{userId, kind}
                : new Object[]{userId};
        if (kind != null) sql.append(" AND kind = ?");
        sql.append(" ORDER BY id");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> rs.getLong("id"), args);
    }

    public long insertResume(long userId, String title, Long targetJobDescriptionId) {
        return insertResume(userId, title, targetJobDescriptionId, "GENERAL", null);
    }

    public long insertResume(long userId, String title, Long targetJobDescriptionId,
                             String kind, Long forkedFromVersionId) {
        jdbcTemplate.update(
                "INSERT INTO resumes (user_id, title, target_job_description_id, kind, forked_from_version_id) VALUES (?, ?, ?, ?, ?)",
                userId, title, targetJobDescriptionId, kind, forkedFromVersionId
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id != null ? id : 0L;
    }

    public String findTitleById(long userId, long id) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT title FROM resumes WHERE id = ? AND user_id = ? AND deleted_at IS NULL",
                    String.class,
                    id, userId
            );
        } catch (EmptyResultDataAccessException missing) {
            return null;
        }
    }

    public String findKindById(long userId, long id) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT kind FROM resumes WHERE id = ? AND user_id = ? AND deleted_at IS NULL",
                    String.class,
                    id, userId
            );
        } catch (EmptyResultDataAccessException missing) {
            return null;
        }
    }

    public Long findForkedFromVersionIdById(long userId, long id) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT forked_from_version_id FROM resumes WHERE id = ? AND user_id = ? AND deleted_at IS NULL",
                    Long.class,
                    id, userId
            );
        } catch (EmptyResultDataAccessException missing) {
            return null;
        }
    }

    public java.time.LocalDateTime findArchivedAtById(long userId, long id) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT archived_at FROM resumes WHERE id = ? AND user_id = ? AND deleted_at IS NULL",
                    java.time.LocalDateTime.class,
                    id, userId
            );
        } catch (EmptyResultDataAccessException missing) {
            return null;
        }
    }

    public void updateTitle(long userId, long resumeId, String title) {
        jdbcTemplate.update(
                "UPDATE resumes SET title = ?, updated_at = NOW(3) WHERE id = ? AND user_id = ? AND deleted_at IS NULL",
                title, resumeId, userId
        );
    }

    public void updateArchivedAt(long userId, long resumeId, java.time.LocalDateTime archivedAt) {
        jdbcTemplate.update(
                "UPDATE resumes SET archived_at = ?, updated_at = NOW(3) WHERE id = ? AND user_id = ? AND deleted_at IS NULL",
                archivedAt, resumeId, userId
        );
    }

    /**
     * 原子创建岗位表达副本：读取到的源正文由调用方传入（服务端复制，renderer 不提交正文）。
     * 创建 JOB_EXPRESSION 资产 + fork V1 + 更新 current_version_id，需在事务内调用。
     */
    public long createForkedAsset(long userId, String title, long sourceVersionId,
                                  int sourceVersionNo, String contentJson) {
        long resumeId = insertResume(userId, title, null, "JOB_EXPRESSION", sourceVersionId);
        long v1Id = insertVersion(
                resumeId,
                null,
                1,
                contentJson,
                "从源版本 V" + sourceVersionNo + " 创建岗位表达副本",
                "fork"
        );
        updateCurrentVersionId(resumeId, v1Id);
        return resumeId;
    }

    public Long findCurrentVersionId(long resumeId) {
        return jdbcTemplate.queryForObject(
                "SELECT current_version_id FROM resumes WHERE id = ?",
                Long.class,
                resumeId
        );
    }

    public Long findTargetJobDescriptionIdById(long userId, long id) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT target_job_description_id FROM resumes WHERE id = ? AND user_id = ? AND deleted_at IS NULL",
                    Long.class,
                    id, userId
            );
        } catch (EmptyResultDataAccessException missing) {
            return null;
        }
    }

    public void updateTargetJobDescriptionId(long resumeId, Long targetJobDescriptionId) {
        jdbcTemplate.update(
                "UPDATE resumes SET target_job_description_id = ?, updated_at = NOW(3) WHERE id = ? AND deleted_at IS NULL",
                targetJobDescriptionId, resumeId
        );
    }

    public ResumeVersionDTO findVersionById(long versionId) {
        List<ResumeVersionDTO> rows = jdbcTemplate.query(
                """
                        SELECT id, resume_id, parent_version_id, version_no, content_json,
                               change_summary, created_by_type, created_at
                        FROM resume_versions
                        WHERE id = ?
                        """,
                versionRowMapper(),
                versionId
        );
        return rows.stream().findFirst().orElse(null);
    }

    /** 按用户读取版本：跨用户按不存在处理（JOIN resumes 校验归属）。 */
    public ResumeVersionDTO findVersionByIdForUser(long userId, long versionId) {
        List<ResumeVersionDTO> rows = jdbcTemplate.query(
                """
                        SELECT rv.id, rv.resume_id, rv.parent_version_id, rv.version_no, rv.content_json,
                               rv.change_summary, rv.created_by_type, rv.created_at
                        FROM resume_versions rv
                        JOIN resumes r ON r.id = rv.resume_id
                        WHERE rv.id = ? AND r.user_id = ? AND r.deleted_at IS NULL
                        """,
                versionRowMapper(),
                versionId, userId
        );
        return rows.stream().findFirst().orElse(null);
    }

    /** 读取版本原始正文 JSON（用于 fork 服务端复制），需在归属校验后调用。 */
    public String findContentJsonById(long versionId) {
        return jdbcTemplate.queryForObject(
                "SELECT content_json FROM resume_versions WHERE id = ?",
                String.class,
                versionId
        );
    }

    public List<ResumeVersionDTO> findVersionsByResumeId(long resumeId) {
        return jdbcTemplate.query(
                """
                        SELECT id, resume_id, parent_version_id, version_no, content_json,
                               change_summary, created_by_type, created_at
                        FROM resume_versions
                        WHERE resume_id = ?
                        ORDER BY version_no DESC
                        """,
                versionRowMapper(),
                resumeId
        );
    }

    public int findMaxVersionNo(long resumeId) {
        Integer max = jdbcTemplate.queryForObject(
                "SELECT MAX(version_no) FROM resume_versions WHERE resume_id = ?",
                Integer.class,
                resumeId
        );
        return max != null ? max : 0;
    }

    public long insertVersion(long resumeId, Long parentVersionId, int versionNo,
                              String contentJson, String changeSummary, String createdByType) {
        jdbcTemplate.update(
                """
                        INSERT INTO resume_versions
                        (resume_id, parent_version_id, version_no, content_json,
                         change_summary, created_by_type)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                resumeId, parentVersionId, versionNo, contentJson, changeSummary, createdByType
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id != null ? id : 0L;
    }

    public void updateVersionChangeSummary(long versionId, String changeSummary) {
        jdbcTemplate.update(
                "UPDATE resume_versions SET change_summary = ? WHERE id = ?",
                changeSummary, versionId
        );
    }

    public void updateCurrentVersionId(long resumeId, long versionId) {
        jdbcTemplate.update(
                "UPDATE resumes SET current_version_id = ?, updated_at = NOW(3) WHERE id = ?",
                versionId, resumeId
        );
    }

    public void replaceEvidenceRefsForVersion(long resumeVersionId, long userId,
                                              List<EvidenceRefDraft> evidenceRefs) {
        jdbcTemplate.update(
                "DELETE FROM resume_evidence_refs WHERE resume_version_id = ?",
                resumeVersionId
        );
        for (EvidenceRefDraft ref : evidenceRefs) {
            jdbcTemplate.update(
                    """
                            INSERT INTO resume_evidence_refs (resume_version_id, evidence_id, section_key)
                            SELECT ?, ce.id, ?
                            FROM capability_evidences ce
                            WHERE ce.id = ?
                              AND ce.user_id = ?
                              AND ce.deleted_at IS NULL
                            """,
                    resumeVersionId,
                    ref.sectionKey(),
                    ref.evidenceId(),
                    userId
            );
        }
    }

    public long createSelfReportedProjectEvidence(long userId, ProjectEvidenceDraft evidence) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO capability_evidences (
                                user_id, evidence_type, title, situation, action_text,
                                result_text, skill_tags, source_note
                            )
                            VALUES (?, 'project', ?, ?, ?, ?, CAST(? AS JSON), ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, userId);
            statement.setString(2, evidence.title());
            statement.setString(3, evidence.situation());
            statement.setString(4, evidence.actionText());
            statement.setString(5, evidence.resultText());
            statement.setString(6, toJson(evidence.skillTags()));
            statement.setString(7, evidence.sourceNote());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("创建项目自述证据失败：未返回主键");
        }
        return key.longValue();
    }

    public boolean existsActiveEvidenceForUser(long userId, long evidenceId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM capability_evidences
                        WHERE id = ?
                          AND user_id = ?
                          AND deleted_at IS NULL
                        """,
                Integer.class,
                evidenceId,
                userId
        );
        return count != null && count > 0;
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("项目证据技能标签格式错误", exception);
        }
    }

    /**
     * 将 suggestedText 应用到 contentJson 中 sectionKey 指定的位置。
     * <p>
     * 支持格式：fieldName、array[N]、field.array[N]、field.array[N].subField。
     * 如果目标是一个 Map（对象），则设置其 "description" 字段。
     */
    @SuppressWarnings("unchecked")
    public String applySuggestion(String contentJson, String sectionKey, String suggestedText) {
        try {
            Map<String, Object> root = objectMapper.readValue(contentJson,
                    new TypeReference<Map<String, Object>>() {});
            Object target = navigateTo(root, sectionKey);
            if (target instanceof Map) {
                ((Map<String, Object>) target).put("description", suggestedText);
            } else {
                setByPath(root, sectionKey, suggestedText);
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("应用建议到简历内容失败: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object navigateTo(Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Object current = root;
        for (String part : parts) {
            String fieldName = part;
            int idx = -1;
            int bracketPos = part.indexOf('[');
            if (bracketPos > 0) {
                fieldName = part.substring(0, bracketPos);
                String idxStr = part.substring(bracketPos + 1, part.length() - 1);
                idx = Integer.parseInt(idxStr);
            }
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(fieldName);
            }
            if (idx >= 0 && current instanceof List) {
                current = ((List<?>) current).get(idx);
            }
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private void setByPath(Map<String, Object> root, String path, Object value) {
        String[] parts = path.split("\\.");
        Object current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            String fieldName = parts[i];
            int idx = -1;
            int bracketPos = fieldName.indexOf('[');
            if (bracketPos > 0) {
                fieldName = fieldName.substring(0, bracketPos);
                String idxStr = parts[i].substring(bracketPos + 1, parts[i].length() - 1);
                idx = Integer.parseInt(idxStr);
            }
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(fieldName);
            }
            if (idx >= 0 && current instanceof List) {
                current = ((List<?>) current).get(idx);
            }
        }
        String lastKey = parts[parts.length - 1];
        int lastBracket = lastKey.indexOf('[');
        if (lastBracket > 0) {
            String fieldName = lastKey.substring(0, lastBracket);
            String idxStr = lastKey.substring(lastBracket + 1, lastKey.length() - 1);
            int idx = Integer.parseInt(idxStr);
            if (current instanceof Map) {
                List<Object> list = (List<Object>) ((Map<String, Object>) current).get(fieldName);
                if (list != null) {
                    list.set(idx, value);
                }
            }
        } else {
            if (current instanceof Map) {
                ((Map<String, Object>) current).put(lastKey, value);
            }
        }
    }

    private RowMapper<ResumeVersionDTO> versionRowMapper() {
        return (rs, rowNum) -> new ResumeVersionDTO(
                rs.getLong("id"),
                rs.getLong("resume_id"),
                rs.getObject("parent_version_id", Long.class),
                rs.getInt("version_no"),
                parseContentJson(rs.getString("content_json")),
                rs.getString("change_summary"),
                rs.getString("created_by_type"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private Map<String, Object> parseContentJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("简历版本 content_json 解析失败", e);
        }
    }

    public int softDelete(long userId, long resumeId) {
        return jdbcTemplate.update(
                """
                UPDATE resumes
                SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                """,
                resumeId, userId
        );
    }
}
