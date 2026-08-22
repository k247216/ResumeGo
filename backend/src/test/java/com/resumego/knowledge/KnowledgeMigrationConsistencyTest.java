package com.resumego.knowledge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeMigrationConsistencyTest {

    private final Path migrationDir = Path.of("src/main/resources/db/migration");
    private final Path h2Dir = Path.of("src/main/resources/db/migration-h2");

    @Test
    void mysqlV26UsesUnsignedBigIntForForeignKeyColumns() throws IOException {
        String sql = Files.readString(migrationDir.resolve("V26__knowledge_library_foundation.sql"), StandardCharsets.UTF_8);
        // users.id 是 BIGINT UNSIGNED，所有 FK 列必须一致，否则 MySQL 拒绝建外键
        assertThat(sql).contains("id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY");
        assertThat(sql).contains("user_id BIGINT UNSIGNED NOT NULL");
        assertThat(sql).contains("document_id BIGINT UNSIGNED NOT NULL");
        assertThat(sql).contains("COLLATE=utf8mb4_unicode_ci");
    }

    @Test
    void repositoryUsesPortableGeneratedKey() throws IOException {
        String repo = Files.readString(
                Path.of("src/main/java/com/resumego/knowledge/KnowledgeRepository.java"),
                StandardCharsets.UTF_8);
        // 显式指定返回列名 + KeyHolder.getKey()：H2 与 MySQL 均可移植
        assertThat(repo).contains("new String[]{\"id\"}");
        assertThat(repo).contains("keys.getKey()");
        assertThat(repo).doesNotContain("RETURN_GENERATED_KEYS");
    }
}
