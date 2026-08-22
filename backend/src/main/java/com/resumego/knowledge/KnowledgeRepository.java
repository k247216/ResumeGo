package com.resumego.knowledge;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class KnowledgeRepository {

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
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("创建知识文档失败：未返回主键");
        return key.longValue();
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
}
