package com.resumego.ai;

import com.resumego.job.dto.ParsedJobDescriptionDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubJdParseServiceTest {

    private final StubJdParseService service = new StubJdParseService();

    @Test
    void shouldExtractRequiredSkills() {
        String rawText = "岗位要求：熟悉 Java，掌握 Spring Boot 和 MySQL，理解 RESTful API 设计。";

        ParsedJobDescriptionDTO result = service.parse(rawText);

        assertThat(result.getRequiredSkills())
                .contains("Java", "Spring Boot", "MySQL", "RESTful API");
    }

    @Test
    void shouldExtractResponsibilities() {
        String rawText = "岗位职责：负责后端业务接口开发，参与数据库表设计和基础联调。";

        ParsedJobDescriptionDTO result = service.parse(rawText);

        assertThat(result.getResponsibilities())
                .isNotEmpty()
                .anyMatch(r -> r.contains("后端业务接口开发"))
                .anyMatch(r -> r.contains("数据库表设计"));
    }

    @Test
    void shouldExtractPreferredSkills() {
        String rawText = "了解 Vue 或前端开发经验优先，有 Spring Boot 项目经验优先。";

        ParsedJobDescriptionDTO result = service.parse(rawText);

        assertThat(result.getPreferredSkills()).isNotEmpty();
    }

    @Test
    void shouldExtractExperienceRequirements() {
        String rawText = "有课程项目或实习项目经验，1 年以上开发经验优先。";

        ParsedJobDescriptionDTO result = service.parse(rawText);

        assertThat(result.getExperienceRequirements()).isNotEmpty();
    }

    @Test
    void shouldExtractEducationRequirements() {
        String rawText = "本科及以上学历，计算机相关专业。";

        ParsedJobDescriptionDTO result = service.parse(rawText);

        assertThat(result.getEducationRequirements()).isNotEmpty();
    }

    @Test
    void shouldReturnEmptyResultsForIrrelevantText() {
        String rawText = "欢迎加入我们的团队，我们提供有竞争力的薪资和福利。";

        ParsedJobDescriptionDTO result = service.parse(rawText);

        // 不应从无关文本中抽取技术技能
        assertThat(result.getRequiredSkills()).isEmpty();
    }
}
