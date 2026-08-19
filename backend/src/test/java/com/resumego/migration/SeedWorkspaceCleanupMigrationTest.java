package com.resumego.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.jdbc.Sql;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Sql(scripts = "/sql/seed_cleanup_schema.sql")
class SeedWorkspaceCleanupMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void removesKnownSeedWorkspaceButPreservesUserCreatedData() {
        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V20__remove_seeded_workspace_data.sql")
        ).execute(dataSource);

        assertThat(count("capability_evidences", 1)).isZero();
        assertThat(count("resumes", 1)).isZero();
        assertThat(count("resume_versions", 1)).isZero();
        assertThat(count("job_descriptions", 10)).isZero();
        assertThat(count("job_matches", 1)).isZero();
        assertThat(count("optimization_suggestions", 1)).isZero();
        assertThat(count("interview_plans", 1)).isZero();
        assertThat(count("interview_sessions", 1)).isZero();
        assertThat(count("interview_growth_snapshots", 1)).isZero();

        assertThat(count("capability_evidences", 50)).isOne();
        assertThat(count("resumes", 50)).isOne();
        assertThat(count("resume_versions", 50)).isOne();
        assertThat(count("job_descriptions", 50)).isOne();
        assertThat(count("job_matches", 50)).isOne();
        assertThat(count("optimization_suggestions", 50)).isOne();
        assertThat(count("interview_plans", 50)).isOne();
        assertThat(count("interview_sessions", 50)).isOne();
        assertThat(count("interview_growth_snapshots", 50)).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT display_name FROM users WHERE id = 1",
                String.class
        )).isEqualTo("本地用户");
    }

    private long count(String table, long id) {
        Long value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE id = ?",
                Long.class,
                id
        );
        return value == null ? 0 : value;
    }
}
