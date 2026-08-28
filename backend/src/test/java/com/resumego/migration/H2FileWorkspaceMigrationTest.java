package com.resumego.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class H2FileWorkspaceMigrationTest {

    @TempDir
    Path tempDir;

    @Test
    void migratesAndReopensAFileWorkspace() throws Exception {
        Path databasePath = tempDir.resolve("workspace");
        String jdbcUrl = "jdbc:h2:file:" + databasePath
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, "sa", "")
                .locations("classpath:db/migration-h2")
                .load();
        flyway.migrate();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("24");

        try (var connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO resumes (user_id, title) VALUES (1, '本地简历')");
            assertThat(tableExists(connection, "job_projects")).isTrue();
            assertThat(tableExists(connection, "job_stage_events")).isTrue();
        }

        try (var connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             var statement = connection.prepareStatement("SELECT title FROM resumes WHERE user_id = 1")) {
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("本地简历");
            }
        }

        var secondMigration = Flyway.configure()
                .dataSource(jdbcUrl, "sa", "")
                .locations("classpath:db/migration-h2")
                .load()
                .migrate();
        assertThat(secondMigration.migrationsExecuted).isZero();
        assertThat(Flyway.configure()
                .dataSource(jdbcUrl, "sa", "")
                .locations("classpath:db/migration-h2")
                .load()
                .info().current().getVersion().getVersion()).isEqualTo("24");
    }

    @Test
    void restoresWorkspaceFromAColdFileBackup() throws Exception {
        Path databasePath = tempDir.resolve("restore-workspace");
        Path databaseFile = tempDir.resolve("restore-workspace.mv.db");
        Path backupFile = tempDir.resolve("restore-workspace.backup.mv.db");
        String jdbcUrl = "jdbc:h2:file:" + databasePath
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";

        Flyway.configure()
                .dataSource(jdbcUrl, "sa", "")
                .locations("classpath:db/migration-h2")
                .load()
                .migrate();
        try (var connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO resumes (user_id, title) VALUES (1, '备份前简历')");
            statement.execute("SHUTDOWN");
        }

        Files.copy(databaseFile, backupFile, StandardCopyOption.REPLACE_EXISTING);

        try (var connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             var statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE resumes SET title = '错误迁移后的内容'");
            statement.execute("SHUTDOWN");
        }
        Files.copy(backupFile, databaseFile, StandardCopyOption.REPLACE_EXISTING);

        try (var connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT title FROM resumes")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).isEqualTo("备份前简历");
        }
    }

    private boolean tableExists(java.sql.Connection connection, String tableName) throws Exception {
        try (var result = connection.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
            return result.next();
        }
    }
}
