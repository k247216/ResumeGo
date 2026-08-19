package com.resumego.job;

import com.resumego.job.dto.CreateJobDescriptionRequest;
import com.resumego.job.dto.JobDescriptionDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Sql(scripts = "/sql/job_descriptions_schema.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class JobDescriptionServiceTest {

    @Autowired
    private JobDescriptionService jobDescriptionService;

    @Autowired
    private JobDescriptionMapper jobDescriptionMapper;

    @Test
    void shouldCreateJobDescription() {
        CreateJobDescriptionRequest req = new CreateJobDescriptionRequest();
        req.setJobTitle("后端开发实习生");
        req.setCompanyName("示例科技");
        req.setRawText("岗位职责：负责后端业务接口开发。岗位要求：熟悉 Java、Spring Boot、MySQL。");

        JobDescriptionDTO result = jobDescriptionService.create(req);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getJobTitle()).isEqualTo("后端开发实习生");
        assertThat(result.getCompanyName()).isEqualTo("示例科技");
        assertThat(result.getParseStatus()).isEqualTo("pending");
        assertThat(result.getParsed()).isNull();
    }

    @Test
    void shouldParseJobDescription() {
        // 先创建一条 JD
        CreateJobDescriptionRequest req = new CreateJobDescriptionRequest();
        req.setJobTitle("Java 开发");
        req.setCompanyName("测试公司");
        req.setRawText("岗位职责：负责后端业务接口开发，参与数据库表设计。"
                + "岗位要求：熟悉 Java，掌握 Spring Boot 和 MySQL，"
                + "了解 Vue 优先，本科及以上学历。");

        JobDescriptionDTO created = jobDescriptionService.create(req);

        // 解析
        JobDescriptionDTO parsed = jobDescriptionService.parse(created.getId());

        assertThat(parsed.getParseStatus()).isEqualTo("succeeded");
        assertThat(parsed.getParsed()).isNotNull();
        assertThat(parsed.getParsed().getRequiredSkills())
                .contains("Java", "Spring Boot", "MySQL");
        assertThat(parsed.getParsed().getResponsibilities()).isNotEmpty();
    }

    @Test
    void shouldReturnNullForNonExistentId() {
        JobDescriptionDTO result = jobDescriptionService.parse(99999L);
        assertThat(result).isNull();
    }

    @Test
    void shouldFindJobDescriptionById() {
        CreateJobDescriptionRequest req = new CreateJobDescriptionRequest();
        req.setJobTitle("测试岗位");
        req.setCompanyName("测试公司");
        req.setRawText("岗位职责：负责系统测试工作。岗位要求：熟悉测试方法论。");

        JobDescriptionDTO created = jobDescriptionService.create(req);

        JobDescriptionDTO found = jobDescriptionService.findById(created.getId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getJobTitle()).isEqualTo("测试岗位");
        assertThat(found.getParseStatus()).isEqualTo("pending");
    }

    @Test
    void shouldListAllJobDescriptions() {
        // 创建 2 条 JD
        CreateJobDescriptionRequest req1 = new CreateJobDescriptionRequest();
        req1.setJobTitle("JD A");
        req1.setRawText("岗位职责：负责后端业务接口开发和数据库设计。岗位要求：熟悉 Java 和 MySQL。");

        CreateJobDescriptionRequest req2 = new CreateJobDescriptionRequest();
        req2.setJobTitle("JD B");
        req2.setRawText("岗位职责：负责前端页面开发。岗位要求：熟悉 Vue 和 TypeScript。");

        jobDescriptionService.create(req1);
        jobDescriptionService.create(req2);

        List<JobDescriptionDTO> list = jobDescriptionService.findAllByUser();
        assertThat(list).hasSizeGreaterThanOrEqualTo(2);
        // 按创建时间倒序，最新的在前面
        assertThat(list.get(0).getJobTitle()).isEqualTo("JD B");
    }

    @Test
    void shouldReturnNullForNonExistentFindById() {
        JobDescriptionDTO result = jobDescriptionService.findById(99999L);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("create: 空 rawText 应抛出异常")
    void shouldRejectEmptyRawText() {
        CreateJobDescriptionRequest req = new CreateJobDescriptionRequest();
        req.setJobTitle("测试");
        req.setRawText("   ");

        assertThatThrownBy(() -> jobDescriptionService.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JD 正文不能为空");
    }

    @Test
    @DisplayName("create: 超长 rawText（>50K 字符）应抛出异常")
    void shouldRejectOversizedRawText() {
        StringBuilder sb = new StringBuilder(50_001);
        for (int i = 0; i < 50_001; i++) {
            sb.append('x');
        }

        CreateJobDescriptionRequest req = new CreateJobDescriptionRequest();
        req.setJobTitle("测试");
        req.setRawText(sb.toString());

        assertThatThrownBy(() -> jobDescriptionService.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JD 正文过长");
    }

    @Test
    @DisplayName("parse: 已存在但正文为空的 JD 应标记为失败而非调用解析器")
    void shouldMarkPersistedEmptyRawTextAsFailed() {
        JobDescription entity = new JobDescription();
        entity.setUserId(1L);
        entity.setJobTitle("历史空 JD");
        entity.setRawText("   ");
        entity.setParseStatus("pending");
        jobDescriptionMapper.insert(entity);

        JobDescriptionDTO result = jobDescriptionService.parse(entity.getId());

        assertThat(result.getParseStatus()).isEqualTo("failed");
        assertThat(result.getParsed()).isNull();
    }

    @Test
    @DisplayName("parse: 损坏的来源元数据应被安全忽略，不阻断 JD 解析")
    void shouldIgnoreMalformedSourceMetaWhenParsing() {
        CreateJobDescriptionRequest req = new CreateJobDescriptionRequest();
        req.setJobTitle("Java 开发实习生");
        req.setRawText("岗位职责：负责后端接口开发。岗位要求：熟悉 Java 和 Spring Boot。");
        req.setSourceMetaJson("{not-json");

        JobDescriptionDTO created = jobDescriptionService.create(req);
        JobDescriptionDTO result = jobDescriptionService.parse(created.getId());

        assertThat(result.getParseStatus()).isEqualTo("succeeded");
        assertThat(result.getParsed()).isNotNull();
        assertThat(result.getParsed().getRequiredSkills()).contains("Java", "Spring Boot");
    }

    @Test
    @DisplayName("delete: 已存在与不存在的 JD 返回稳定结果")
    void shouldDeleteExistingJobAndReturnFalseForMissingJob() {
        CreateJobDescriptionRequest req = new CreateJobDescriptionRequest();
        req.setJobTitle("待删除岗位");
        req.setRawText("岗位职责：维护后端服务。岗位要求：了解 Java。");
        JobDescriptionDTO created = jobDescriptionService.create(req);

        assertThat(jobDescriptionService.delete(created.getId())).isTrue();
        assertThat(jobDescriptionService.findById(created.getId())).isNull();
        assertThat(jobDescriptionService.delete(created.getId())).isFalse();
    }

    @Test
    @DisplayName("batchUpdateJobType: 仅更新演示用户的现有 JD 并返回更新数量")
    void shouldBatchUpdateJobType() {
        CreateJobDescriptionRequest first = new CreateJobDescriptionRequest();
        first.setJobTitle("岗位 A");
        first.setRawText("岗位职责：处理服务端任务。岗位要求：熟悉 Java。");
        CreateJobDescriptionRequest second = new CreateJobDescriptionRequest();
        second.setJobTitle("岗位 B");
        second.setRawText("岗位职责：处理前端任务。岗位要求：熟悉 TypeScript。");
        JobDescriptionDTO firstCreated = jobDescriptionService.create(first);
        JobDescriptionDTO secondCreated = jobDescriptionService.create(second);

        int updated = jobDescriptionService.batchUpdateJobType("campus");

        assertThat(updated).isGreaterThanOrEqualTo(2);
        assertThat(jobDescriptionService.findById(firstCreated.getId()).getJobType()).isEqualTo("campus");
        assertThat(jobDescriptionService.findById(secondCreated.getId()).getJobType()).isEqualTo("campus");
    }
}
