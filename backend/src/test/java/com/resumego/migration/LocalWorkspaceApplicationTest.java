package com.resumego.migration;

import com.resumego.ai.provider.AiProviderProfileRequest;
import com.resumego.ai.provider.AiProviderProfileService;
import com.resumego.project.JobProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.sql.init.mode=never"
)
@ActiveProfiles("local")
class LocalWorkspaceApplicationTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void localDatabase(DynamicPropertyRegistry registry) {
        String url = "jdbc:h2:file:" + tempDir.resolve("application-workspace")
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
        registry.add("spring.datasource.url", () -> url);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    JobProjectRepository jobProjectRepository;

    @Autowired
    AiProviderProfileService aiProviderProfileService;

    @Test
    void startsWithCleanPersonalWorkspaceAndUsableRepositories() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT display_name FROM users WHERE id = 1", String.class
        )).isEqualTo("Local User");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM resumes", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM job_descriptions", Long.class)).isZero();
        assertThat(jobProjectRepository.findAll(1L)).isEmpty();

        var first = aiProviderProfileService.create(new AiProviderProfileRequest(
                "DeepSeek", "openai-compatible", "https://api.deepseek.com/v1", "deepseek-chat"));
        var second = aiProviderProfileService.create(new AiProviderProfileRequest(
                "Anthropic", "anthropic", "https://api.anthropic.com/v1", "claude-test"));
        aiProviderProfileService.setDefault(first.id());
        aiProviderProfileService.setDefault(second.id());
        assertThat(aiProviderProfileService.list()).filteredOn(profile -> profile.defaultProfile()).hasSize(1)
                .first().extracting(profile -> profile.id()).isEqualTo(second.id());
    }
}
