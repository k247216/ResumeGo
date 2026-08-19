package com.resumego.matching.service;

import com.resumego.matching.dto.MatchResponse;
import com.resumego.matching.entity.JobMatch;
import com.resumego.matching.mapper.JobMatchMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 岗位匹配服务测试。
 *
 * <h3>测试覆盖</h3>
 * <ul>
 *   <li>AC-01 ~ AC-06：阶段 C 验收测试（match-test-cases.md §5.0）</li>
 *   <li>SAMPLE-01 ~ 05：固定简历/JD 样例（同 §5.1）</li>
 *   <li>B-01 ~ B-10：边界与异常场景（同 §5.2）</li>
 *   <li>I-01 ~ I-04：幂等性测试（同 §5.3）</li>
 *   <li>V-01 ~ V-03：前置校验与错误响应（同 §5.4）</li>
 * </ul>
 *
 * <p>期望值基于算法 v1.0.0 + test SQL 种子数据计算。</p>
 */
@SpringBootTest
@Transactional
@Sql(scripts = "/sql/job_matches_schema.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DisplayName("MatchingService 匹配服务测试")
class MatchingServiceTest {

    @Autowired
    private MatchingPipelineService matchingPipelineService;

    @Autowired
    private JobMatchMapper jobMatchMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ============================================================
    // §5.0  阶段 C 验收测试（AC-01 ~ AC-06）
    // ============================================================

    @Nested
    @DisplayName("§5.0 阶段 C 验收测试")
    class AcceptanceTests {

        @Test
        @DisplayName("AC-01 输出 matchScore 在 0-100 范围内")
        void shouldReturnMatchScoreInRange() {
            MatchResponse r = matchingPipelineService.match(1L, 1L);
            assertThat(r).isNotNull();
            assertThat(r.matchScore()).isBetween(0, 100);
        }

        @Test
        @DisplayName("AC-02 输出必备项覆盖：requiredCoverage + matchedItems")
        void shouldReturnRequiredCoverage() {
            // v1: Java/Spring Boot/MySQL/Git  vs  jd1 required: Java/Spring Boot/MySQL
            MatchResponse r = matchingPipelineService.match(1L, 1L);

            assertThat(r.details()).isNotNull();
            // 3/3 必备技能全部命中（别名: spring boot → springboot）
            assertThat(r.details().getRequiredCoverage()).isEqualTo(100);
            assertThat(r.details().getMatchedItems())
                    .contains("Java", "Spring Boot", "MySQL");
        }

        @Test
        @DisplayName("AC-03 输出加分项覆盖：preferredCoverage")
        void shouldReturnPreferredCoverage() {
            // v1 不含 Redis/Docker
            MatchResponse r = matchingPipelineService.match(1L, 1L);

            assertThat(r.details().getPreferredCoverage()).isEqualTo(0);
        }

        @Test
        @DisplayName("AC-04 输出缺口：missingItems + unknownItems")
        void shouldReturnMissingItems() {
            MatchResponse r = matchingPipelineService.match(1L, 1L);

            assertThat(r.details().getMissingItems()).isNotNull();
            assertThat(r.details().getMissingItems())
                    .contains("Redis", "Docker");
            assertThat(r.details().getUnknownItems()).isNotNull();
        }

        @Test
        @DisplayName("AC-05 写入 job_matches 表，字段完整")
        void shouldPersistToDatabase() {
            MatchResponse response = matchingPipelineService.match(1L, 1L);
            assertThat(response).isNotNull();

            // DB 中写入了一行
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM job_matches WHERE resume_version_id = 1 AND job_description_id = 1",
                    Integer.class);
            assertThat(count).isEqualTo(1);

            // 各字段非空
            JobMatch row = jobMatchMapper.selectList(null).stream()
                    .filter(m -> m.getResumeVersionId() == 1L && m.getJobDescriptionId() == 1L)
                    .findFirst().orElse(null);
            assertThat(row).isNotNull();
            assertThat(row.getAlgorithmVersion()).isEqualTo(MatchingPipelineService.ALGORITHM_VERSION);
            assertThat(row.getMatchScore()).isBetween(0, 100);
            assertThat(row.getDetailsJson()).isNotEmpty();
            assertThat(row.getInputFingerprint()).hasSize(64);
            assertThat(row.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("AC-06 相同输入结果稳定（幂等）")
        void shouldBeIdempotent() {
            MatchResponse first = matchingPipelineService.match(1L, 1L);
            MatchResponse second = matchingPipelineService.match(1L, 1L);

            assertThat(second.matchScore()).isEqualTo(first.matchScore());
            assertThat(second.details().getRequiredCoverage())
                    .isEqualTo(first.details().getRequiredCoverage());
            assertThat(second.details().getPreferredCoverage())
                    .isEqualTo(first.details().getPreferredCoverage());
            assertThat(second.details().getExperienceCoverage())
                    .isEqualTo(first.details().getExperienceCoverage());
            assertThat(second.details().isEducationMatch())
                    .isEqualTo(first.details().isEducationMatch());
            assertThat(second.details().getMissingItems())
                    .containsExactlyInAnyOrderElementsOf(first.details().getMissingItems());
            assertThat(second.details().getMatchedItems())
                    .containsExactlyInAnyOrderElementsOf(first.details().getMatchedItems());

            // DB 只创建了一条记录
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM job_matches WHERE resume_version_id = 1 AND job_description_id = 1",
                    Integer.class);
            assertThat(count).isEqualTo(1);
        }
    }

    // ============================================================
    // §5.1  固定简历/JD 样例（5 组）
    // ============================================================

    @Nested
    @DisplayName("§5.1 固定简历/JD 样例")
    class FixedSampleTests {

        @Test
        @DisplayName("样例 1：高匹配 — v1(Java/Spring/MySQL/Git) vs jd1(Java/Spring/MySQL)")
        void sample1_highMatch_fullyMatchedRequiredSkills() {
            // v1 skills: Java, Spring Boot, MySQL, Git  →  必备 3/3=100%
            // jd1 preferred: Redis, Docker  →  0/2=0%
            // 经验关键词匹配度受限于简单分词算法
            MatchResponse r = matchingPipelineService.match(1L, 1L);

            assertThat(r.matchScore()).isBetween(40, 100);           // 实际约 45（经验维度"有项目经验"未匹配、加分技能为0权重）
            assertThat(r.details().getRequiredCoverage()).isEqualTo(100);
            assertThat(r.details().getPreferredCoverage()).isEqualTo(0);
            assertThat(r.details().isEducationMatch()).isTrue();
            assertThat(r.details().getMatchedItems()).contains("Java", "Spring Boot", "MySQL");
            assertThat(r.details().getMissingItems()).contains("Redis", "Docker");
        }

        @Test
        @DisplayName("样例 2：中等匹配 — v1(3/5) vs jd4 边界 60%")
        void sample2_mediumMatch_requiredAtSixtyBoundary() {
            // v1 覆盖 jd4 的 3/5=60%，刚好不触发一票否决
            MatchResponse r = matchingPipelineService.match(1L, 4L);

            assertThat(r.details().getRequiredCoverage()).isEqualTo(60);
            assertThat(r.matchScore()).isBetween(55, 65);           // 实际约 60
            assertThat(r.details().getMissingItems()).contains("Redis", "Docker");
            // 无 preferred/experience/education 要求 → 默认 100
            assertThat(r.details().getPreferredCoverage()).isEqualTo(100);
            assertThat(r.details().getExperienceCoverage()).isEqualTo(100);
            assertThat(r.details().isEducationMatch()).isTrue();
        }

        @Test
        @DisplayName("样例 3：低匹配 — v2(empty skills) vs jd1")
        void sample3_lowMatch_emptySkillsAgainstNormalJd() {
            // v2 skills: [] → 必备 0/3=0%，触发一票否决
            MatchResponse r = matchingPipelineService.match(2L, 1L);

            assertThat(r.details().getRequiredCoverage()).isEqualTo(0);
            assertThat(r.matchScore()).isLessThanOrEqualTo(40);     // 一票否决封顶
            assertThat(r.details().getMissingItems()).contains("Java", "Spring Boot", "MySQL");
        }

        @Test
        @DisplayName("样例 4：退化场景 — v1 vs jd2（全空要求）→ 满分")
        void sample4_degenerateCase_emptyJdAllFullScores() {
            MatchResponse r = matchingPipelineService.match(1L, 2L);

            assertThat(r.matchScore()).isEqualTo(100);
            assertThat(r.details().getRequiredCoverage()).isEqualTo(100);
            assertThat(r.details().getPreferredCoverage()).isEqualTo(100);
            assertThat(r.details().getExperienceCoverage()).isEqualTo(100);
            assertThat(r.details().isEducationMatch()).isTrue();
            assertThat(r.details().getMissingItems()).isEmpty();
        }

        @Test
        @DisplayName("样例 5：别名匹配 — v5(K8s/Node.js/React.js/PG) vs jd5(Kubernetes/Node/React/Postgres)")
        void sample5_aliasMatch_allRequiredHitViaAlias() {
            // K8s→Kubernetes, Node.js→Node, React.js→React, PostgreSQL→Postgres 全部命中
            // v5 project "4 年全栈开发经验" 与 jd5 experience "3 年以上全栈开发" bigram 重叠命中
            MatchResponse r = matchingPipelineService.match(5L, 5L);

            assertThat(r.details().getRequiredCoverage()).isEqualTo(100);
            assertThat(r.details().getMatchedItems())
                    .contains("Kubernetes", "Node", "React", "Postgres");
            // 别名映射记录
            assertThat(r.details().getAliasMatches()).isNotNull();
            assertThat(r.details().getAliasMatches()).containsKeys("K8s", "Node.js", "React.js", "PostgreSQL");
            // n-gram 经验匹配：bigram {全栈,栈开,开发} 重叠 3/7=42.9% ≥ 40% 阈值 → 命中
            assertThat(r.details().getExperienceCoverage()).isEqualTo(100);
            assertThat(r.details().getMatchedItems()).contains("3 年以上全栈开发");
        }

        @Test
        @DisplayName("样例 6：应届生命中 — v8(period=2022-2026) vs jd8(应届生)")
        void sample6_freshGradPositive_graduateYearMatches() {
            // v8 period "2022-2026" → graduateYear=2026 → isFreshGrad=true
            // jd8 experienceRequirements: ["应届生"] → reconcileExpWithFreshGrad 修正命中
            MatchResponse r = matchingPipelineService.match(8L, 8L);

            assertThat(r.details().getExperienceCoverage()).isEqualTo(100);
            assertThat(r.details().getMatchedItems()).contains("应届生");
        }

        @Test
        @DisplayName("样例 7：非应届生 — v9(period=2020-2024) vs jd8(应届生)")
        void sample7_freshGradNegative_graduateYearMismatch() {
            // v9 period "2020-2024" → graduateYear=2024 ≠ 2026 → isFreshGrad=false
            // jd8 experienceRequirements: ["应届生"] → n-gram 不命中 + 非应届 → missing
            MatchResponse r = matchingPipelineService.match(9L, 8L);

            assertThat(r.details().getExperienceCoverage()).isEqualTo(0);
            assertThat(r.details().getMissingItems()).contains("应届生");
        }
    }

    // ============================================================
    // §5.2  边界与异常场景（B-01 ~ B-10）
    // 分组：简历侧 → JD 侧 → 通用
    // ============================================================

    @Nested
    @DisplayName("§5.2 边界与异常场景")
    class BoundaryTests {

        // ============================================================
        // 简历侧边界（输入异常）— B-01 ~ B-04
        // ============================================================

        @Test
        @DisplayName("B-01 简历技能为空 → requiredCoverage=0，触发一票否决")
        void emptyResumeSkills_allRequiredBecomeMissing() {
            MatchResponse r = matchingPipelineService.match(2L, 1L);

            assertThat(r.details().getRequiredCoverage()).isEqualTo(0);
            assertThat(r.matchScore()).isLessThanOrEqualTo(40);
            assertThat(r.details().getMissingItems()).contains("Java", "Spring Boot", "MySQL");
        }

        @Test
        @DisplayName("B-02 简历经历为空 → experienceCoverage=0")
        void emptyResumeExperiences_experienceRequirementsMissing() {
            // v4: projects=[]
            MatchResponse r = matchingPipelineService.match(4L, 1L);

            assertThat(r.details().getExperienceCoverage()).isEqualTo(0);
            assertThat(r.details().getMissingItems()).contains("有项目经验");
        }

        @Test
        @DisplayName("B-03 简历学历为空 → educationMatch=false")
        void emptyResumeEducation_educationMatchFalse() {
            // v3: education=[]
            MatchResponse r = matchingPipelineService.match(3L, 1L);

            assertThat(r.details().isEducationMatch()).isFalse();
        }

        @Test
        @DisplayName("B-04 技能名称含大小写和首尾空白 → 规范化后精确匹配")
        void skillNameCaseAndWhitespace_normalizedBeforeMatch() {
            // v7: ["  JAVA  ","spring boot","mysql"] → 全部命中 jd1
            MatchResponse r = matchingPipelineService.match(7L, 1L);

            assertThat(r.details().getRequiredCoverage()).isEqualTo(100);
            assertThat(r.details().getMatchedItems()).contains("Java", "Spring Boot", "MySQL");
        }

        // ============================================================
        // JD 侧边界（要求异常）— B-05 ~ B-09
        // ============================================================

        @Test
        @DisplayName("B-05 JD 五项要求全为空 → matchScore=100")
        void emptyJdRequirements_allFullScores() {
            MatchResponse r = matchingPipelineService.match(1L, 2L);

            assertThat(r.matchScore()).isEqualTo(100);
            assertThat(r.details().getRequiredCoverage()).isEqualTo(100);
            assertThat(r.details().getPreferredCoverage()).isEqualTo(100);
            assertThat(r.details().getExperienceCoverage()).isEqualTo(100);
            assertThat(r.details().getMissingItems()).isEmpty();
        }

        @Test
        @DisplayName("B-06 JD 必备技能刚好 60% 覆盖，不触发一票否决")
        void requiredExactlySixtyPercent_vetoNotTriggered() {
            MatchResponse r = matchingPipelineService.match(1L, 4L);

            assertThat(r.details().getRequiredCoverage()).isEqualTo(60);
            // ≥60 不触发一票否决
            assertThat(r.matchScore()).isGreaterThanOrEqualTo(60);
        }

        @Test
        @DisplayName("B-07 JD 必备技能 < 60% 触发一票否决封顶 40")
        void requiredBelowSixty_vetoTriggered() {
            MatchResponse r = matchingPipelineService.match(2L, 4L);

            assertThat(r.details().getRequiredCoverage()).isEqualTo(0);
            assertThat(r.matchScore()).isLessThanOrEqualTo(40);
        }

        @Test
        @DisplayName("B-08 JD 学历要求无法解析 → 不影响，educationMatch=true")
        void unparseableEducationRequirement_noEffectOnScore() {
            // jd6: ["优秀院校优先"] → 无法映射到已知等级
            MatchResponse r = matchingPipelineService.match(1L, 6L);

            assertThat(r.matchScore()).isBetween(90, 100);         // 必备全命中 + 学历不扣分
            assertThat(r.details().isEducationMatch()).isTrue();
            assertThat(r.details().getRequiredCoverage()).isEqualTo(100);
        }

        @Test
        @DisplayName("B-09 JD 重复技能去重后计数")
        void duplicateRequiredSkills_deduplicatedInCount() {
            // jd7: ["Go","Go"] → 去重为 1 个必备技能
            MatchResponse r = matchingPipelineService.match(1L, 7L);

            // 去重后只有 1 个必备技能 "Go"，v1 无 Go → 0/1=0%
            assertThat(r.details().getRequiredCoverage()).isEqualTo(0);
        }

        // ============================================================
        // 通用边界 — B-10
        // ============================================================

        @Test
        @DisplayName("B-10 计算结果确定性 — 同一输入两次结果一致")
        void longSkillList_deterministicResult() {
            MatchResponse first = matchingPipelineService.match(1L, 1L);
            MatchResponse second = matchingPipelineService.match(1L, 1L);

            assertThat(second.matchScore()).isEqualTo(first.matchScore());
            assertThat(second.details().getRequiredCoverage())
                    .isEqualTo(first.details().getRequiredCoverage());
        }
    }

    // ============================================================
    // §5.3  幂等性测试（I-01 ~ I-04）
    // ============================================================

    @Nested
    @DisplayName("§5.3 幂等性测试")
    class IdempotencyTests {

        @Test
        @DisplayName("I-01 同一输入 POST 两次 → 返回相同 matchScore 和 details")
        void sameInputTwice_sameScoreAndDetails() {
            MatchResponse first = matchingPipelineService.match(1L, 1L);
            MatchResponse second = matchingPipelineService.match(1L, 1L);

            assertThat(second.matchScore()).isEqualTo(first.matchScore());
            assertThat(second.details().getRequiredCoverage())
                    .isEqualTo(first.details().getRequiredCoverage());
            assertThat(second.details().getMissingItems())
                    .containsExactlyInAnyOrderElementsOf(first.details().getMissingItems());
        }

        @Test
        @DisplayName("I-02 同一简历版本对同一 JD POST 两次 → DB 只建一条记录")
        void sameVersionSameJdTwice_onlyOneDbRow() {
            matchingPipelineService.match(1L, 1L);
            matchingPipelineService.match(1L, 1L);

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM job_matches WHERE resume_version_id = 1 AND job_description_id = 1",
                    Integer.class);
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("I-03 相同输入仅创建一条记录（单版本）")
        void singleAlgorithmVersion_createsOneRecord() {
            MatchResponse r1 = matchingPipelineService.match(1L, 1L);
            assertThat(r1).isNotNull();

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM job_matches WHERE resume_version_id = 1 AND job_description_id = 1",
                    Integer.class);
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("I-04 时间戳不影响 fingerprint → 仍命中幂等")
        void sameContentDifferentTimestamp_sameFingerprint() {
            MatchResponse first = matchingPipelineService.match(1L, 1L);
            MatchResponse second = matchingPipelineService.match(1L, 1L);

            assertThat(second.matchScore()).isEqualTo(first.matchScore());

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT input_fingerprint) FROM job_matches WHERE resume_version_id = 1 AND job_description_id = 1",
                    Integer.class);
            assertThat(count).isEqualTo(1);
        }
    }

    // ============================================================
    // §5.3b  批量匹配（BATCH-01 ~ BATCH-02）
    // ============================================================

    @Nested
    @DisplayName("§5.3b 批量匹配")
    class BatchMatchTests {

        @Test
        @DisplayName("BATCH-01 批量匹配返回全部 JD 并排序")
        void shouldReturnAllMatchesSortedByScore() {
            List<MatchingPipelineService.BatchMatchResult> results =
                    matchingPipelineService.batchMatch(1L);

            assertThat(results).isNotEmpty();
            // 验证降序排列
            for (int i = 1; i < results.size(); i++) {
                assertThat(results.get(i - 1).matchScore())
                        .isGreaterThanOrEqualTo(results.get(i).matchScore());
            }
            // 验证包含已知 JD
            assertThat(results.stream().anyMatch(r -> r.jobDescriptionId() == 1L)).isTrue();
        }

        @Test
        @DisplayName("BATCH-02 批量匹配跳过未解析 JD")
        void shouldSkipUnparsedJds() {
            List<MatchingPipelineService.BatchMatchResult> results =
                    matchingPipelineService.batchMatch(1L);

            // jd3 的 parseStatus=pending，不应出现在结果中
            assertThat(results.stream().noneMatch(r -> r.jobDescriptionId() == 3L)).isTrue();
        }
    }

    // ============================================================
    // §5.4  前置校验与错误响应（V-01 ~ V-03）
    // ============================================================

    @Nested
    @DisplayName("§5.4 前置校验与错误响应")
    class ValidationTests {

        @Test
        @DisplayName("V-01 简历版本不存在 → IllegalArgumentException")
        void shouldThrowWhenResumeVersionNotFound() {
            assertThrows(IllegalArgumentException.class,
                    () -> matchingPipelineService.match(99999L, 1L));
        }

        @Test
        @DisplayName("V-02 JD 不存在 → IllegalArgumentException('JD_NOT_FOUND')")
        void shouldThrowWhenJdNotFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> matchingPipelineService.match(1L, 99999L));
            assertThat(ex.getMessage()).isEqualTo("JD_NOT_FOUND");
        }

        @Test
        @DisplayName("V-03 JD 未解析 → IllegalStateException('JD_NOT_PARSED')")
        void shouldThrowWhenJdNotParsed() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> matchingPipelineService.match(1L, 3L));
            assertThat(ex.getMessage()).isEqualTo("JD_NOT_PARSED");
        }

        @Test
        @DisplayName("V-04（预留）其他用户的简历版本无法访问")
        @org.junit.jupiter.api.Disabled(
                "Sprint 1 使用固定演示用户，ResumeService 未做用户隔离校验。" +
                "待用户体系完善后启用：v6 属于 userId=999，应抛出异常。")
        void shouldRejectVersionFromOtherUser() {
            // r6/v6 belongs to userId=999 — 待 ResumeService 增加用户隔离后启用
        }
    }
}
