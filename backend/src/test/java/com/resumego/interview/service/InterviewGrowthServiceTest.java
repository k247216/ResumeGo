package com.resumego.interview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.interview.dto.MultiSessionSummaryResponse;
import com.resumego.interview.entity.InterviewPlan;
import com.resumego.interview.entity.InterviewSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("InterviewGrowthService 单元测试")
class InterviewGrowthServiceTest {

    private ObjectMapper objectMapper;
    private final InterviewGrowthService service = new InterviewGrowthService(
            mock(JdbcTemplate.class),
            new ObjectMapper()
    );

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("应基于已保存的单题评价分数生成成长快照草稿")
    void shouldBuildSnapshotDraftFromQuestionScores() {
        InterviewPlan plan = new InterviewPlan();
        plan.setId(100L);
        plan.setResumeVersionId(10L);
        plan.setJobDescriptionId(20L);

        MultiSessionSummaryResponse summary = new MultiSessionSummaryResponse(
                "整体表达稳定，但项目影响力和技术取舍还需要补充。",
                78,
                List.of("表达结构清晰"),
                List.of("项目影响力表达不足", "技术取舍不够具体"),
                List.of("用 STAR 法补充项目结果"),
                List.of()
        );
        LocalDateTime completedAt = LocalDateTime.of(2026, 7, 15, 22, 30);

        InterviewGrowthService.SnapshotDraft draft = service.buildSnapshotDraft(
                plan,
                List.of(
                        "{\"clarity\":8,\"relevance\":7,\"depth\":6,\"accuracy\":8}",
                        "{\"clarity\":6,\"relevance\":8,\"depth\":7,\"accuracy\":7}"
                ),
                summary,
                completedAt
        );

        assertThat(draft.resumeVersionId()).isEqualTo(10L);
        assertThat(draft.jobDescriptionId()).isEqualTo(20L);
        assertThat(draft.interviewPlanId()).isEqualTo(100L);
        assertThat(draft.clarityScore()).isEqualTo(7.0);
        assertThat(draft.relevanceScore()).isEqualTo(7.5);
        assertThat(draft.depthScore()).isEqualTo(6.5);
        assertThat(draft.accuracyScore()).isEqualTo(7.5);
        assertThat(draft.overallScore()).isEqualTo(7.1);
        assertThat(draft.weakPointsJson()).contains("项目影响力表达不足");
        assertThat(draft.summaryText()).contains("整体表达稳定");
        assertThat(draft.completedAt()).isEqualTo(completedAt);
    }

    @Test
    @DisplayName("应将已完成计划写入成长快照并返回趋势报告")
    void shouldUpsertSnapshotAndReturnGrowthReport() {
        JdbcTemplate jdbcTemplate = createJdbcTemplate();
        InterviewGrowthService jdbcService = new InterviewGrowthService(jdbcTemplate, objectMapper);
        seedBaseData(jdbcTemplate, 100L, 10L, 20L, 99L, null);
        seedCompletedSessionWithEvaluation(jdbcTemplate, 100L, 1001L, 1,
                "{\"clarity\":8,\"relevance\":7,\"depth\":6,\"accuracy\":8}");
        seedCompletedSessionWithEvaluation(jdbcTemplate, 100L, 1002L, 2,
                "{\"clarity\":6,\"relevance\":8,\"depth\":7,\"accuracy\":7}");

        InterviewPlan plan = plan(100L, 10L, 20L);
        MultiSessionSummaryResponse summary = summary("整体表达稳定", "项目影响力表达不足");
        jdbcService.upsertSnapshotForPlan(plan, List.of(
                completedSession(1001L, LocalDateTime.of(2026, 7, 15, 21, 0)),
                completedSession(1002L, LocalDateTime.of(2026, 7, 15, 22, 0))
        ), summary);

        var report = jdbcService.getGrowthReport(99L, 20L);

        assertThat(report.resumeId()).isEqualTo(99L);
        assertThat(report.jobTitle()).isEqualTo("后端开发实习生");
        assertThat(report.companyName()).isEqualTo("字节跳动");
        assertThat(report.snapshots()).hasSize(1);
        assertThat(report.snapshots().getFirst().resumeVersionId()).isEqualTo(10L);
        assertThat(report.snapshots().getFirst().versionLabel()).contains("v3", "用户编辑");
        assertThat(report.snapshots().getFirst().dimensions().clarity()).isEqualTo(7.0);
        assertThat(report.snapshots().getFirst().dimensions().relevance()).isEqualTo(7.5);
        assertThat(report.snapshots().getFirst().dimensions().depth()).isEqualTo(6.5);
        assertThat(report.snapshots().getFirst().dimensions().accuracy()).isEqualTo(7.5);
        assertThat(report.snapshots().getFirst().summary()).contains("整体表达稳定");
        assertThat(report.changes().clarity()).isZero();
    }

    @Test
    @DisplayName("查询趋势时应补写已有整次复盘但缺少快照的历史计划")
    void shouldBackfillMissingSnapshotFromSavedSummary() throws Exception {
        JdbcTemplate jdbcTemplate = createJdbcTemplate();
        InterviewGrowthService jdbcService = new InterviewGrowthService(jdbcTemplate, objectMapper);
        String summaryJson = objectMapper.writeValueAsString(summary("历史复盘已保存", "技术深度不足"));
        seedBaseData(jdbcTemplate, 101L, 11L, 21L, 199L, summaryJson);
        seedCompletedSessionWithEvaluation(jdbcTemplate, 101L, 1011L, 1,
                "{\"clarity\":9,\"relevance\":8,\"depth\":7,\"accuracy\":8}");

        var report = jdbcService.getGrowthReport(199L, 21L);

        assertThat(report.snapshots()).hasSize(1);
        assertThat(report.snapshots().getFirst().representativePlanId()).isEqualTo(101L);
        assertThat(report.snapshots().getFirst().dimensions().clarity()).isEqualTo(9.0);
        assertThat(report.snapshots().getFirst().summary()).contains("历史复盘已保存");
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM interview_growth_snapshots WHERE interview_plan_id = 101",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("岗位不存在时趋势报告应返回兜底岗位名称")
    void shouldUseFallbackJobBriefWhenJobIsMissing() {
        JdbcTemplate jdbcTemplate = createJdbcTemplate();
        InterviewGrowthService jdbcService = new InterviewGrowthService(jdbcTemplate, objectMapper);

        var report = jdbcService.getGrowthReport(1L, 404L);

        assertThat(report.jobTitle()).isEqualTo("岗位 #404");
        assertThat(report.companyName()).isEmpty();
        assertThat(report.snapshots()).isEmpty();
    }

    private JdbcTemplate createJdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:growth_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        createSchema(jdbcTemplate);
        return jdbcTemplate;
    }

    private void createSchema(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE resume_versions (
                    id BIGINT PRIMARY KEY,
                    resume_id BIGINT NOT NULL,
                    version_no INT NOT NULL,
                    created_by_type VARCHAR(32) NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE job_descriptions (
                    id BIGINT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    job_title VARCHAR(255),
                    company_name VARCHAR(255)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE interview_plans (
                    id BIGINT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    resume_version_id BIGINT NOT NULL,
                    job_description_id BIGINT NOT NULL,
                    title VARCHAR(255),
                    question_count INT,
                    persona_plan_json CLOB,
                    focus_tags_json CLOB,
                    supplement_text CLOB,
                    summary_json CLOB,
                    summary_generated_at TIMESTAMP,
                    deleted_at TIMESTAMP,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE interview_sessions (
                    id BIGINT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    resume_version_id BIGINT NOT NULL,
                    job_description_id BIGINT NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    current_question_index INT,
                    total_questions INT,
                    started_at TIMESTAMP,
                    completed_at TIMESTAMP,
                    summary_json CLOB,
                    persona_id BIGINT,
                    persona_name VARCHAR(255),
                    persona_title VARCHAR(255),
                    plan_id BIGINT,
                    round_order INT,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE interview_evaluations (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    session_id BIGINT NOT NULL,
                    score_json CLOB,
                    created_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE interview_growth_snapshots (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    resume_id BIGINT NOT NULL,
                    resume_version_id BIGINT NOT NULL,
                    job_description_id BIGINT NOT NULL,
                    interview_plan_id BIGINT NOT NULL UNIQUE,
                    clarity_score DOUBLE,
                    relevance_score DOUBLE,
                    depth_score DOUBLE,
                    accuracy_score DOUBLE,
                    overall_score DOUBLE,
                    weak_points_json CLOB,
                    summary_text CLOB,
                    completed_at TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    private void seedBaseData(JdbcTemplate jdbcTemplate,
                              long planId,
                              long resumeVersionId,
                              long jobDescriptionId,
                              long resumeId,
                              String summaryJson) {
        jdbcTemplate.update(
                "INSERT INTO resume_versions (id, resume_id, version_no, created_by_type) VALUES (?, ?, ?, ?)",
                resumeVersionId, resumeId, 3, "user"
        );
        jdbcTemplate.update(
                "INSERT INTO job_descriptions (id, user_id, job_title, company_name) VALUES (?, 1, ?, ?)",
                jobDescriptionId, "后端开发实习生", "字节跳动"
        );
        jdbcTemplate.update("""
                INSERT INTO interview_plans (
                    id, user_id, resume_version_id, job_description_id, title, question_count,
                    persona_plan_json, focus_tags_json, supplement_text, summary_json,
                    summary_generated_at, deleted_at, created_at, updated_at
                ) VALUES (?, 1, ?, ?, '多轮模拟面试', 3, '[]', '[]', '', ?, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, planId, resumeVersionId, jobDescriptionId, summaryJson);
    }

    private void seedCompletedSessionWithEvaluation(JdbcTemplate jdbcTemplate,
                                                    long planId,
                                                    long sessionId,
                                                    int roundOrder,
                                                    String scoreJson) {
        jdbcTemplate.update("""
                INSERT INTO interview_sessions (
                    id, user_id, resume_version_id, job_description_id, status,
                    current_question_index, total_questions, started_at, completed_at,
                    summary_json, persona_id, persona_name, persona_title, plan_id,
                    round_order, created_at, updated_at
                ) VALUES (?, 1, 10, 20, 'COMPLETED', 3, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                    '{}', 1, '技术面试官', '后端专家', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, sessionId, planId, roundOrder);
        jdbcTemplate.update(
                "INSERT INTO interview_evaluations (session_id, score_json, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
                sessionId, scoreJson
        );
    }

    private InterviewPlan plan(long planId, long resumeVersionId, long jobDescriptionId) {
        InterviewPlan plan = new InterviewPlan();
        plan.setId(planId);
        plan.setUserId(1L);
        plan.setResumeVersionId(resumeVersionId);
        plan.setJobDescriptionId(jobDescriptionId);
        return plan;
    }

    private InterviewSession completedSession(long id, LocalDateTime completedAt) {
        InterviewSession session = new InterviewSession();
        session.setId(id);
        session.setStatus("COMPLETED");
        session.setCompletedAt(completedAt);
        return session;
    }

    private MultiSessionSummaryResponse summary(String overallSummary, String weakness) {
        return new MultiSessionSummaryResponse(
                overallSummary,
                78,
                List.of("表达结构清晰"),
                List.of(weakness),
                List.of("用 STAR 法补充项目结果"),
                List.of()
        );
    }
}
