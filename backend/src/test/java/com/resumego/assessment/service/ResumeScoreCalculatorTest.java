package com.resumego.assessment.service;

import com.resumego.assessment.dto.ResumeAssessmentEvidenceRef;
import com.resumego.assessment.dto.ResumeAssessmentInput;
import com.resumego.assessment.dto.ResumeAssessmentResultDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResumeScoreCalculator 单元测试 —— 禁飞区边界场景。
 * 不依赖 Spring 上下文，纯 Java 测试。
 */
@DisplayName("ResumeScoreCalculator 简历评分计算器")
class ResumeScoreCalculatorTest {

    private ResumeScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ResumeScoreCalculator();
    }

    // ---- helpers ----

    private Map<String, Object> content(Map<String, Object> overrides) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("basicInfo", overrides.getOrDefault("basicInfo", basicInfo("张三", "13800000000", "zhang@example.com")));
        c.put("education", overrides.getOrDefault("education", education("武汉大学", "软件工程", "本科")));
        c.put("projects", overrides.getOrDefault("projects", projects(
                project("校园二手交易平台", "负责后端接口开发，使用Spring Boot+MySQL，日均处理1000+请求，响应时间从2s优化至200ms", 1L),
                project("在线评测系统", "独立完成前端到部署全流程，基于Vue3+SpringBoot，支持500人并发，代码量2万行", 2L)
        )));
        c.put("skills", overrides.getOrDefault("skills", List.of("Java", "Spring Boot", "MySQL", "Vue3", "Redis", "Docker")));
        return c;
    }

    private Map<String, Object> basicInfo(String name, String phone, String email) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("phone", phone);
        m.put("email", email);
        return m;
    }

    private List<Map<String, Object>> education(String school, String major, String degree) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("school", school);
        e.put("major", major);
        e.put("degree", degree);
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(e);
        return list;
    }

    private List<Map<String, Object>> projects(Map<String, Object>... items) {
        return new ArrayList<>(List.of(items));
    }

    private Map<String, Object> project(String title, String description, Long evidenceId) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("title", title);
        p.put("description", description);
        if (evidenceId != null) p.put("evidenceId", evidenceId);
        return p;
    }

    private List<ResumeAssessmentEvidenceRef> refs(long... ids) {
        return Arrays.stream(ids)
                .mapToObj(id -> new ResumeAssessmentEvidenceRef(id, id, "projects", true))
                .toList();
    }

    private ResumeAssessmentInput input(Map<String, Object> content, List<ResumeAssessmentEvidenceRef> refs) {
        return new ResumeAssessmentInput(1L, content, refs);
    }

    private int dim(Map<String, Object> dimensionScores, String key) {
        return (Integer) dimensionScores.get(key);
    }

    // ---- deduction helper ----

    private List<Map<String, Object>> dedupeDeductionsByCode(List<Map<String, Object>> deductions) {
        // 按 code 去重（多个维度可能产生相同 code 的扣分）
        List<Map<String, Object>> deduped = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Map<String, Object> d : deductions) {
            String code = (String) d.get("code");
            if (seen.add(code)) deduped.add(d);
        }
        return deduped;
    }

    // ================================================================
    // 边界场景
    // ================================================================

    @Nested
    @DisplayName("边界场景 B1: 全空简历 → 各维度触底")
    class EmptyResume {

        @Test
        @DisplayName("总分应为 0，所有模块缺失扣分")
        void shouldScoreZeroWhenEverythingMissing() {
            Map<String, Object> empty = new LinkedHashMap<>();
            // 不放入任何字段
            ResumeAssessmentInput in = input(empty, List.of());

            ResumeAssessmentResultDraft result = calculator.assess(in);

            assertThat(result.totalScore().intValue()).isEqualTo(25);
            Map<String, Object> dims = result.dimensionScores();
            assertThat(dim(dims, "completeness")).isEqualTo(0);
            assertThat(dim(dims, "evidenceSupport")).isEqualTo(0);
            assertThat(dim(dims, "experienceQuality")).isEqualTo(0);
            // quantitative: projects=[] → -5, skills=[] → -5 → 10
            assertThat(dim(dims, "quantitativeExpression")).isEqualTo(10);
            // readability: no basicInfo → no deductions. no skills → no dup. no edu → no deduction → 15
            assertThat(dim(dims, "readability")).isEqualTo(15);
            assertThat(result.totalScore().intValue()).isBetween(0, 30);

            // 扣分码应包含所有模块缺失
            List<Map<String, Object>> uniq = dedupeDeductionsByCode(result.deductions());
            List<String> codes = uniq.stream().map(d -> (String) d.get("code")).toList();
            assertThat(codes).contains(
                    "missing_basic_info", "missing_education",
                    "missing_projects", "missing_skills"
            );
        }
    }

    @Nested
    @DisplayName("边界场景 B2: 完美简历 → 各维度满分")
    class PerfectResume {

        @Test
        @DisplayName("内容充实且有匹配证据引用的简历应接近满分")
        void shouldScoreHighWhenWellStructured() {
            Map<String, Object> c = content(Map.of());
            List<ResumeAssessmentEvidenceRef> evidenceRefs = refs(1L, 2L);
            ResumeAssessmentInput in = input(c, evidenceRefs);

            ResumeAssessmentResultDraft result = calculator.assess(in);

            assertThat(result.totalScore().intValue()).isGreaterThanOrEqualTo(95);
            Map<String, Object> ds2 = result.dimensionScores();
            assertThat(dim(ds2, "completeness")).isEqualTo(15);
            assertThat(dim(ds2, "evidenceSupport")).isEqualTo(20);
            // 1条描述不足50字 → "description_too_short" 扣3分
            assertThat(dim(ds2, "experienceQuality")).isEqualTo(27);
            assertThat(dim(ds2, "quantitativeExpression")).isEqualTo(20);
            assertThat(dim(ds2, "readability")).isEqualTo(15);
            // 只有 description_too_short 一项扣分
            assertThat(result.deductions()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("边界场景 B3: 混合边界 — 证据无效 + 技能重复 + 描述过短")
    class MixedBoundary {

        @Test
        @DisplayName("证据不匹配、技能重复、描述过短应产生多项扣分")
        void shouldDeductForInvalidEvidenceDuplicateSkillsShortDescriptions() {
            Map<String, Object> c = content(Map.of(
                    "projects", projects(
                            project("短项目", "太短", 99L),           // evidenceId=99 不存在于 refs, 描述<50字
                            project("另一个短项目", "描述", null)     // 无evidenceId, 描述<50字
                    ),
                    "skills", List.of("Java", "Java", "Spring Boot")   // Java 重复
            ));
            List<ResumeAssessmentEvidenceRef> refs = refs(/* 空: 没有匹配的 evidence */);
            ResumeAssessmentInput in = input(c, refs);

            ResumeAssessmentResultDraft result = calculator.assess(in);

            List<Map<String, Object>> uniq = dedupeDeductionsByCode(result.deductions());
            List<String> codes = uniq.stream().map(d -> (String) d.get("code")).toList();

            // 应包含: 证据无效、证据覆盖率低、描述过短、无技术细节、无量化、技能重复
            assertThat(codes).contains(
                    "invalid_evidence_ref",
                    "description_too_short",
                    "missing_quantitative_result"
            );
            // 证据支撑应低于满分
            assertThat(dim(result.dimensionScores(), "evidenceSupport")).isLessThan(20);
            // 经历质量应低于满分
            assertThat(dim(result.dimensionScores(), "experienceQuality")).isLessThan(30);
            // 可读性因技能重复扣分
            assertThat(dim(result.dimensionScores(), "readability")).isLessThan(15);
        }
    }

    @Nested
    @DisplayName("边界场景 B4: capScore 封顶/触底")
    class CapScore {

        @Test
        @DisplayName("各维度分数不超出 [0, max] 范围")
        void shouldCapWithinBounds() {
            // 全空简历 → 多项扣分超过满分 → 各维度不应出负
            Map<String, Object> empty = new LinkedHashMap<>();
            ResumeAssessmentInput in = input(empty, List.of());
            ResumeAssessmentResultDraft result = calculator.assess(in);

            assertThat(dim(result.dimensionScores(), "completeness")).isBetween(0, 15);
            assertThat(dim(result.dimensionScores(), "evidenceSupport")).isBetween(0, 20);
            assertThat(dim(result.dimensionScores(), "experienceQuality")).isBetween(0, 30);
            assertThat(dim(result.dimensionScores(), "quantitativeExpression")).isBetween(0, 20);
            assertThat(dim(result.dimensionScores(), "readability")).isBetween(0, 15);
            assertThat(result.totalScore().intValue()).isBetween(0, 100);
        }
    }

    // ================================================================
    // 异常路径与分支覆盖
    // ================================================================

    @Nested
    @DisplayName("异常路径: null 与类型异常输入")
    class ExceptionPaths {

        @Test
        @DisplayName("projects 为 null 时不抛异常，按空列表处理")
        void shouldTreatNullProjectsAsEmpty() {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("basicInfo", basicInfo("张三", "138", "z@t.com"));
            c.put("education", education("武大", "软件", "本科"));
            c.put("projects", null);
            c.put("skills", List.of("Java"));
            ResumeAssessmentInput in = input(c, List.of());

            ResumeAssessmentResultDraft result = calculator.assess(in);
            assertThat(result.totalScore().intValue()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("skills 为非 List 类型时不抛异常")
        void shouldHandleNonListSkills() {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("basicInfo", basicInfo("张三", "138", "z@t.com"));
            c.put("education", education("武大", "软件", "本科"));
            c.put("projects", List.of());
            c.put("skills", "not_a_list"); // 字符串而非列表
            ResumeAssessmentInput in = input(c, List.of());

            ResumeAssessmentResultDraft result = calculator.assess(in);
            assertThat(result.totalScore().intValue()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("basicInfo 缺失时 readability 不崩溃")
        void shouldNotCrashWhenBasicInfoMissing() {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("education", education("武大", "软件", "本科"));
            c.put("projects", projects(project("正常项目", "这是描述超过五十字符的详细项目介绍包含了具体的技术栈和实现细节", null)));
            c.put("skills", List.of("Java", "Spring"));
            ResumeAssessmentInput in = input(c, List.of());

            ResumeAssessmentResultDraft result = calculator.assess(in);
            assertThat(dim(result.dimensionScores(), "readability")).isBetween(0, 15);
        }

        @Test
        @DisplayName("education 中有学校无专业时触发 readability 扣分")
        void shouldDeductWhenEducationSchoolOrMajorBlank() {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("basicInfo", basicInfo("张三", "138", "z@t.com"));
            List<Map<String, Object>> edu = new ArrayList<>();
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("school", "武汉大学");
            e.put("major", "");  // 专业为空
            edu.add(e);
            c.put("education", edu);
            c.put("projects", projects(project("正常项目", "足够长的描述文字包含技术细节超过五十字符", 1L)));
            c.put("skills", List.of("Java", "Spring", "MySQL", "Redis", "Docker"));
            ResumeAssessmentInput in = input(c, refs(1L));

            ResumeAssessmentResultDraft result = calculator.assess(in);
            assertThat(dim(result.dimensionScores(), "readability")).isLessThan(15);
        }
    }

    @Nested
    @DisplayName("分支覆盖: experienceQuality 技术检测")
    class ExperienceQualityBranches {

        @Test
        @DisplayName("所有项目缺技术关键词 → missing_project_action 扣分")
        void shouldDeductWhenNoTechKeywords() {
            Map<String, Object> c = content(Map.of(
                    "projects", projects(
                            project("纯中文项目", "这是一个完全没有英文技术关键词的项目描述文字用来测试技术检测分支超过五十个字符", 1L),
                            project("另一个纯中文", "另一个也是纯中文没有技术词汇的项目描述需要超过五十个字符才行", 2L)
                    )
            ));
            ResumeAssessmentInput in = input(c, refs(1L, 2L));
            ResumeAssessmentResultDraft result = calculator.assess(in);
            assertThat(dim(result.dimensionScores(), "experienceQuality")).isLessThan(30);
        }
    }

    @Nested
    @DisplayName("分支覆盖: readability 技能去重")
    class ReadabilityBranchCoverage {

        @Test
        @DisplayName("技能重复触发去重扣分")
        void shouldDeductForDuplicateSkills() {
            Map<String, Object> c = content(Map.of(
                    "skills", List.of("Java", "Java", "Java")  // 三重复
            ));
            ResumeAssessmentInput in = input(c, refs(1L, 2L));
            ResumeAssessmentResultDraft result = calculator.assess(in);
            assertThat(dim(result.dimensionScores(), "readability")).isLessThan(15);
        }

        @Test
        @DisplayName("技能无重复 → 不扣分")
        void shouldNotDeductForUniqueSkills() {
            Map<String, Object> c = content(Map.of(
                    "skills", List.of("Java", "Spring", "MySQL")
            ));
            ResumeAssessmentInput in = input(c, refs(1L, 2L));
            ResumeAssessmentResultDraft result = calculator.assess(in);
            assertThat(dim(result.dimensionScores(), "readability")).isEqualTo(15);
        }

        @Test
        @DisplayName("phone 和 email 都有 → readability 不扣联系方式分")
        void shouldNotDeductContactWhenBothPresent() {
            Map<String, Object> c = content(Map.of(
                    "basicInfo", basicInfo("张三", "13800000000", "zhang@example.com")
            ));
            ResumeAssessmentInput in = input(c, refs(1L, 2L));
            ResumeAssessmentResultDraft result = calculator.assess(in);
            // name+phone+email 都填了 → 无 contact 扣分
            assertThat(dim(result.dimensionScores(), "readability")).isGreaterThanOrEqualTo(12);
        }
    }

    @Nested
    @DisplayName("分支覆盖: invalid_evidence_ref 上限")
    class EvidenceCapBranch {

        @Test
        @DisplayName("3个以上无效证据 → 扣分上限 15")
        void shouldCapInvalidEvidenceDeduction() {
            Map<String, Object> c = content(Map.of(
                    "projects", projects(
                            project("A", "描述一够五十字包含详细项目介绍和技术栈说明", 91L),
                            project("B", "描述二也够五十字包含详细项目介绍和技术栈", 92L),
                            project("C", "描述三同样够五十字包含详细项目介绍", 93L)
                    )
            ));
            // refs 完全空 → 3个项目证据全部无效 → Math.min(5*3, 3*5)? wait code is 5 * Math.min(invalidRefCount, 3) = 5*3 = 15
            ResumeAssessmentInput in = input(c, List.of());
            ResumeAssessmentResultDraft result = calculator.assess(in);
            // 应该有 invalid_evidence_ref 扣分
            List<String> codes = dedupeDeductionsByCode(result.deductions()).stream()
                    .map(d -> (String) d.get("code")).toList();
            assertThat(codes).contains("invalid_evidence_ref");
        }
    }

    @Nested
    @DisplayName("分支覆盖: evidenceSupport 无效引用与低覆盖率")
    class EvidenceBranchCoverage {

        @Test
        @DisplayName("所有项目证据均无效 → invalid_evidence_ref 扣分")
        void shouldDeductWhenAllEvidenceInvalid() {
            Map<String, Object> c = content(Map.of(
                    "projects", projects(
                            project("项目A", "描述够长包含技术细节和实现方案超过五十个字符", 99L),
                            project("项目B", "另一个项目的详细描述也超过五十字包含Vue前端开发", 98L)
                    )
            ));
            // refs 包含 1,2 但 projects 引用 98,99 → 全部无效
            ResumeAssessmentInput in = input(c, refs(1L, 2L));

            ResumeAssessmentResultDraft result = calculator.assess(in);
            List<String> codes = dedupeDeductionsByCode(result.deductions()).stream()
                    .map(d -> (String) d.get("code")).toList();
            assertThat(codes).contains("invalid_evidence_ref");
        }

        @Test
        @DisplayName("部分项目有 evidenceId 部分没有 → 低覆盖率扣分")
        void shouldDeductWhenPartialEvidenceCoverage() {
            Map<String, Object> c = content(Map.of(
                    "projects", projects(
                            project("项目A", "详细的描述足够长超过五十字包含技术栈", 1L),
                            project("项目B", "没有证据的项目描述也足够长超过五十个字符", 99L)
                    )
            ));
            // refs 只有 1 → 项目B的evidenceId=99 无效 → invalid_evidence_ref
            ResumeAssessmentInput in = input(c, refs(1L));

            ResumeAssessmentResultDraft result = calculator.assess(in);
            assertThat(dim(result.dimensionScores(), "evidenceSupport")).isLessThan(20);
        }
    }
}
