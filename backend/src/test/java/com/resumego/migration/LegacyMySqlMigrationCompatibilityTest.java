package com.resumego.migration;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyMySqlMigrationCompatibilityTest {

    private final Path migrationDir = Path.of("src/main/resources/db/migration");

    @Test
    void keepsAppliedCareerPipelineMigrationsResolvable() throws Exception {
        String pipeline = read("V24__career_pipeline_foundation.sql");
        String links = read("V25__pipeline_asset_links.sql");

        assertThat(pipeline)
                .contains("CREATE TABLE career_pipelines")
                .contains("CREATE TABLE pipeline_stages")
                .contains("CREATE TABLE pipeline_stage_transitions");
        assertThat(links)
                .contains("CREATE TABLE pipeline_schedule_events")
                .contains("CREATE TABLE pipeline_interview_plans");
    }

    private String read(String filename) throws Exception {
        Path path = migrationDir.resolve(filename);
        assertThat(Files.exists(path)).as("Flyway migration %s", filename).isTrue();
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
