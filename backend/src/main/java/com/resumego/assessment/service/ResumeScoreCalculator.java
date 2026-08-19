package com.resumego.assessment.service;

import com.resumego.assessment.dto.ResumeAssessmentEvidenceRef;
import com.resumego.assessment.dto.ResumeAssessmentInput;
import com.resumego.assessment.dto.ResumeAssessmentResultDraft;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * No-Fly-Zone: 简历多维度评分核心逻辑。
 *
 * <p>本类必须由成员 1 根据 harness-docs/architecture/scoring-and-matching-design.md
 * 中的人工规则手写实现。AI 只允许维护调用边界、数据结构和非核心流程，不得生成本方法的
 * 评分权重、扣分规则、阈值判断、总分合成代码或可直接翻译的伪代码。</p>
 */
@Component
public class ResumeScoreCalculator {

    private record DimensionScore(
            String name,
            int score,
            List<Map<String, Object>> deductions
    ) {
    }

    private record ResumeScoreContext(
            Map<String, Object> content,
            List<ResumeAssessmentEvidenceRef> evidenceRefs
    ) {
    }

    private Map<String, Object> deduction(
            String code,
            String dimension,
            String reason,
            int points,
            String rule,
            String suggestion
    ){
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("dimension", dimension);
        item.put("reason", reason);
        item.put("points", points);
        item.put("rule", rule);
        item.put("suggestion", suggestion);
        return item;
    }
    private boolean isMissing(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return text.isBlank();
        }
        if (value instanceof List<?> list) {
            return list.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        return false;
    }
//核心方法（用input作为输入去返回draft的对象）
    public ResumeAssessmentResultDraft assess(ResumeAssessmentInput input) {
        ResumeScoreContext context = new ResumeScoreContext(
                input.content(),
                input.evidenceRefs()
        );
//用dimension score去承接不同维度
        DimensionScore completeness = assessCompleteness(context);
        DimensionScore evidenceSupport = assessEvidenceSupport(context);
        DimensionScore experienceQuality = assessExperienceQuality(context);
        DimensionScore quantitativeExpression = assessQuantitativeExpression(context);
        DimensionScore readability = assessReadability(context);

//计算总分
        int total = completeness.score()
                + evidenceSupport.score()
                + experienceQuality.score()
                + quantitativeExpression.score()
                + readability.score();

//构造返回值的其他参数
        Map<String, Object> dimensionScores = new LinkedHashMap<>();
        dimensionScores.put("completeness", completeness.score());
        dimensionScores.put("evidenceSupport", evidenceSupport.score());
        dimensionScores.put("experienceQuality", experienceQuality.score());
        dimensionScores.put("quantitativeExpression", quantitativeExpression.score());
        dimensionScores.put("readability", readability.score());

        List<Map<String, Object>> deductions = new ArrayList<>();
        deductions.addAll(completeness.deductions());
        deductions.addAll(evidenceSupport.deductions());
        deductions.addAll(experienceQuality.deductions());
        deductions.addAll(quantitativeExpression.deductions());
        deductions.addAll(readability.deductions());

        return new ResumeAssessmentResultDraft(
                BigDecimal.valueOf(total),
                dimensionScores,
                deductions
        );
    }

//完整度检测
    private DimensionScore assessCompleteness(ResumeScoreContext context) {
        List<Map<String, Object>> deductions = new ArrayList<>();
        int score = 15;

        Map<String, Object> content = context.content();

        // 1. 基本信息
        if (isMissing(content.get("basicInfo"))) {
            int points = 5;
            score -= points;
            deductions.add(deduction(
                    "missing_basic_info",
                    "completeness",
                    "基本信息缺失",
                    points,
                    "基本信息应包含姓名、求职意向",
                    "建议补充姓名和求职意向"
            ));
        }

        // 2. 教育背景
        if (isMissing(content.get("education"))) {
            int points = 8;
            score -= points;
            deductions.add(deduction(
                    "missing_education",
                    "completeness",
                    "教育背景缺失",
                    points,
                    "教育背景应包含学校、专业、学历",
                    "建议补充教育经历"
            ));
        }

        // 3. 项目/实习经历
        if (isMissing(content.get("projects"))) {
            int points = 10;
            score -= points;
            deductions.add(deduction(
                    "missing_projects",
                    "completeness",
                    "项目或实习经历缺失",
                    points,
                    "简历应至少包含一段项目或实习经历",
                    "建议从能力证据中关联项目经历"
            ));
        }

        // 4. 技能标签
        if (isMissing(content.get("skills"))) {
            int points = 6;
            score -= points;
            deductions.add(deduction(
                    "missing_skills",
                    "completeness",
                    "技能标签缺失",
                    points,
                    "技能标签不能为空",
                    "建议从能力证据中提取技能标签"
            ));
        }

        score = capScore(score, 15);
        return new DimensionScore("completeness", score, deductions);
    }


    private DimensionScore assessEvidenceSupport(ResumeScoreContext context) {
        List<Map<String, Object>> deductions = new ArrayList<>();
        int score = 20;

        Map<String, Object> content = context.content();
        List<?> projects = content.get("projects") instanceof List ? (List<?>) content.get("projects") : List.of();
        List<ResumeAssessmentEvidenceRef> refs = context.evidenceRefs();

        // 1. 无证据关联的项目数
        if (projects.isEmpty()) {
            score = 0;
            deductions.add(deduction(
                    "no_evidence_ref", "evidenceSupport",
                    "无项目经历，无法评估证据支撑", 20,
                    "简历应包含至少一条项目经历",
                    "建议从能力证据中关联项目经历"
            ));
        } else {
            long unmatchedCount = projects.stream()
                    .filter(p -> p instanceof Map<?, ?> m
                            && m.get("evidenceId") instanceof Number
                            && refs.stream().noneMatch(r -> r.evidenceId().equals(((Number)
                            m.get("evidenceId")).longValue())))
                    .count();

            if (unmatchedCount > 0) {
                int points = (int) (5 * unmatchedCount);
                score -= points;
                deductions.add(deduction(
                        "partial_evidence_ref", "evidenceSupport",
                        unmatchedCount + " 条项目经历未关联能力证据",
                        points,
                        "每条项目经历应有关联的能力证据",
                        "建议补充相关经历的能力证据"
                ));
            }
            // 无效证据引用：ref 存在但 evidence 已被删除或不存在
            long invalidRefCount = projects.stream()
                    .filter(p -> p instanceof Map<?, ?> m
                            && m.get("evidenceId") instanceof Number)
                    .filter(p -> {
                        Number evidenceId = (Number) ((Map<?, ?>) p).get("evidenceId");
                        return refs.stream().noneMatch(r -> r.evidenceId().equals(evidenceId.longValue()));
                    })
                    .count();

            if (invalidRefCount > 0) {
                int points = (int) (5 * Math.min(invalidRefCount, 3));
                score -= points;
                deductions.add(deduction(
                        "invalid_evidence_ref",
                        "evidenceSupport",
                        invalidRefCount + " 条项目经历引用的能力证据无效或已被删除",
                        points,
                        "项目经历必须引用有效的能力证据",
                        "建议重新关联有效的能力证据，或先创建对应的能力证据"
                ));
            }

            // 2. 证据覆盖率低
            long matchedCount = projects.size() - unmatchedCount;
            if (projects.size() > 0 && matchedCount * 2 < projects.size()) {
                int points = 5;
                score -= points;
                deductions.add(deduction(
                        "partial_evidence_ref", "evidenceSupport",
                        "超过半数项目经历缺少证据支撑",
                        points,
                        "证据覆盖率应超过 50%",
                        "建议补充更多能力证据并关联"
                ));
            }
        }

        score = capScore(score, 20);
        return new DimensionScore("evidenceSupport", score, deductions);
    }

    private DimensionScore assessExperienceQuality(ResumeScoreContext context) {
        List<Map<String, Object>> deductions = new ArrayList<>();
        int score = 30;

        Map<String, Object> content = context.content();
        List<?> projects = content.get("projects") instanceof List ? (List<?>) content.get("projects") : List.of();

        if (projects.isEmpty()) {
            score = 0;
            deductions.add(deduction(
                    "missing_projects", "experienceQuality",
                    "无项目或实习经历", 30,
                    "简历应包含项目或实习经历",
                    "建议添加项目经历以展示实际能力"
            ));
        } else {
            // 1. 描述过短
            int shortCount = 0;
            for (Object p : projects) {
                if (p instanceof Map<?, ?> m) {
                    Object descValue = m.get("description");
                    String desc = descValue instanceof String text ? text : "";
                    if (desc.length() < 50) shortCount++;
                }
            }
            if (shortCount > 0) {
                int points = Math.min(10, 3 * shortCount);
                score -= points;
                deductions.add(deduction(
                        "description_too_short", "experienceQuality",
                        shortCount + " 条经历描述过短（少于50字）",
                        points,
                        "经历描述应包含具体技术、行动和成果",
                        "建议使用 STAR 原则展开经历描述"
                ));
            }

            // 2. 缺少技术关键词
            long techRichCount = projects.stream()
                    .filter(p -> p instanceof Map<?, ?> m)  // Java 16 pattern matching not needed
                    .filter(p -> {
                        Object descValue = ((Map<?, ?>) p).get("description");
                        String desc = descValue instanceof String text ? text : "";
                        return desc.matches(".*[（(]?[A-Za-z#.+]+[)）]?.*") || desc.length() > 100;
                    })
                    .count();
            if (projects.size() > 0 && techRichCount < projects.size()) {
                int points = 5;
                score -= points;
                deductions.add(deduction(
                        "missing_project_action", "experienceQuality",
                        "部分经历缺少具体技术细节",
                        points,
                        "经历描述应包含具体使用的技术栈和工具",
                        "建议在描述中补充使用的技术框架和版本"
                ));
            }
        }

        score = capScore(score, 30);
        return new DimensionScore("experienceQuality", score, deductions);
    }

    private DimensionScore assessQuantitativeExpression(ResumeScoreContext context) {
        List<Map<String, Object>> deductions = new ArrayList<>();
        int score = 20;

        Map<String, Object> content = context.content();
        List<?> projects = content.get("projects") instanceof List ? (List<?>) content.get("projects") : List.of();
        List<?> skills = content.get("skills") instanceof List ? (List<?>) content.get("skills") : List.of();

        // 1. 项目数量
        if (projects.size() <= 1) {
            int points = 5;
            score -= points;
            deductions.add(deduction(
                    "missing_projects", "quantitativeExpression",
                    "仅 " + projects.size() + " 条项目经历，数量偏少",
                    points,
                    "2 条以上项目经历更有竞争力",
                    "建议至少补充 2-3 条项目经历"
            ));
        }

        // 2. 技能数量
        if (skills.size() < 5) {
            int points = 5;
            score -= points;
            deductions.add(deduction(
                    "missing_skills", "quantitativeExpression",
                    "技能标签仅 " + skills.size() + " 项",
                    points,
                    "技能标签应覆盖主要技术栈",
                    "建议补充更多相关技能"
            ));
        }

        // 3. 量化结果检查
        long hasQuantified = projects.stream()
                .filter(p -> p instanceof Map<?, ?> m)
                .filter(p -> {
                    Object descValue = ((Map<?, ?>) p).get("description");
                    String desc = descValue instanceof String text ? text : "";
                    return desc.matches(".*\\d+.*");
                })
                .count();
        if (projects.size() > 0 && hasQuantified < projects.size()) {
            int points = Math.min(10, 5 * (int) (projects.size() - hasQuantified));
            score -= points;
            deductions.add(deduction(
                    "missing_quantitative_result", "quantitativeExpression",
                    "部分经历缺少量化结果",
                    points,
                    "经历描述应包含数字、百分比等量化指标",
                    "建议补充如：处理量级、优化幅度、用户规模等数据"
            ));
        }

        score = capScore(score, 20);
        return new DimensionScore("quantitativeExpression", score, deductions);
    }

    private DimensionScore assessReadability(ResumeScoreContext context) {
        List<Map<String, Object>> deductions = new ArrayList<>();
        int score = 15;

        Map<String, Object> content = context.content();

        // 1. 基本信息完整性
        Map<?, ?> basicInfo = content.get("basicInfo") instanceof Map ? (Map<?, ?>) content.get("basicInfo") : null;
        if (basicInfo != null) {
            Object phoneValue = basicInfo.get("phone");
            Object emailValue = basicInfo.get("email");
            Object nameValue = basicInfo.get("name");
            String phone = phoneValue instanceof String text ? text : "";
            String email = emailValue instanceof String text ? text : "";
            String name = nameValue instanceof String text ? text : "";

            if (name.isBlank()) {
                int points = 3;
                score -= points;
                deductions.add(deduction(
                        "missing_basic_info", "readability",
                        "姓名为空", points,
                        "基本信息中应包含姓名",
                        "请填写姓名"
                ));
            }
            if (phone.isBlank() && email.isBlank()) {
                int points = 2;
                score -= points;
                deductions.add(deduction(
                        "missing_basic_info", "readability",
                        "手机和邮箱均为空，缺少联系方式", points,
                        "至少填写一种联系方式",
                        "建议补充手机号或邮箱"
                ));
            }
        }

        // 2. 技能是否有重复
        List<?> skills = content.get("skills") instanceof List ? (List<?>) content.get("skills") : List.of();
        if (skills.size() != skills.stream().distinct().count()) {
            int points = 3;
            score -= points;
            deductions.add(deduction(
                    "missing_skills", "readability",
                    "技能标签存在重复项", points,
                    "技能标签应去重",
                    "建议移除重复的技能标签"
            ));
        }

        // 3. 教育背景中的空字段
        List<?> education = content.get("education") instanceof List ? (List<?>) content.get("education") : List.of();
        if (!education.isEmpty()) {
            boolean hasIncomplete = education.stream()
                    .filter(e -> e instanceof Map<?, ?> m)
                    .anyMatch(e -> {
                        Object schoolValue = ((Map<?, ?>) e).get("school");
                        Object majorValue = ((Map<?, ?>) e).get("major");
                        String school = schoolValue instanceof String text ? text : "";
                        String major = majorValue instanceof String text ? text : "";
                        return school.isBlank() || major.isBlank();
                    });
            if (hasIncomplete) {
                int points = 2;
                score -= points;
                deductions.add(deduction(
                        "missing_education", "readability",
                        "教育背景信息不完整", points,
                        "学校、专业不应为空",
                        "建议补全教育信息"
                ));
            }
        }

        score = capScore(score, 15);
        return new DimensionScore("readability", score, deductions);
    }


//封顶方法
    private int capScore(int score, int max) {
        if (score < 0) {
            return 0;
        }
        if (score > max) {
            return max;
        }
        return score;
    }


}
