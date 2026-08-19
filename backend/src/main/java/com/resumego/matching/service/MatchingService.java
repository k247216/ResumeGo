package com.resumego.matching.service;

import com.resumego.job.dto.ParsedJobDescriptionDTO;
import com.resumego.matching.dto.MatchDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 核心匹配算法服务，负责：
 * <ol>
 *   <li>技能和条件归一化</li>
 *   <li>必备项、加分项、经验等分项匹配</li>
 *   <li>相似度计算和加权</li>
 *   <li>排序、并列和缺失条件规则</li>
 * </ol>
 *
 * <p>编排逻辑（数据加载、缓存、持久化、响应构建）由 {@link MatchingPipelineService} 负责。</p>
 */
@Slf4j
@Service
public class MatchingService {

    static final Map<String, Double> BASE_WEIGHT = Map.of(
            "requiredSkill", 0.40,   // 必备技能
            "preferredSkill", 0.00,  // 加分技能（参考维度）
            "experience",    0.35,   // 经验要求
            "education",     0.20,   // 学历要求
            "dutyCover",     0.05    // 职责覆盖
    );

    /** 字符级 bigram 重叠匹配的阈值。JD 文本的 bigram 与简历文本的重叠比例 ≥ 此值时视为命中。 */
    static final double NGRAM_OVERLAP_THRESHOLD = 0.40;

    /**
     * 执行核心匹配算法。
     *
     * @param resumeSkillList 简历技能列表（已从 content 提取）
     * @param resumeExpText   简历经历文本列表（已从 content 提取）
     * @param resumeEdu       简历最高学历（已从 content 提取）
     * @param resumeDutyWords 简历文本分词结果（已从 content 提取并分词）
     * @param parsed          JD 结构化解析结果
     * @return 算法执行结果（最终分数 + 完整明细）
     */
    public MatchResult executeMatching(
            List<String> resumeSkillList,
            List<String> resumeExpText,
            String resumeEdu,
            int graduateYear,
            List<String> resumeDutyTexts,
            ParsedJobDescriptionDTO parsed) {

        // ---- 1. 从 JD 解析结果中读取各维度要求 ----
        List<String> jdRequired  = dedupe(orEmpty(parsed.getRequiredSkills()));
        List<String> jdPreferred = dedupe(orEmpty(parsed.getPreferredSkills()));
        List<String> jdExpReq    = orEmpty(parsed.getExperienceRequirements());
        List<String> jdEduReq    = orEmpty(parsed.getEducationRequirements());
        List<String> jdDutyTexts = orEmpty(parsed.getResponsibilities());

        // ---- 2. 动态权重分配 ----
        Map<String, Boolean> dimHasReq = new LinkedHashMap<>();
        dimHasReq.put("requiredSkill",  !jdRequired.isEmpty());
        dimHasReq.put("preferredSkill", !jdPreferred.isEmpty());
        dimHasReq.put("experience",     !jdExpReq.isEmpty());
        dimHasReq.put("education",      !jdEduReq.isEmpty());
        dimHasReq.put("dutyCover",      !jdDutyTexts.isEmpty());

        double totalValidWeight = 0.0;
        Map<String, Double> validBase = new HashMap<>(BASE_WEIGHT);
        for (String key : validBase.keySet()) {
            if (!dimHasReq.get(key)) {
                validBase.put(key, 0.0);  // JD 无此要求 → 权重归零
            } else {
                totalValidWeight += validBase.get(key);
            }
        }

        // 按比例重新分配，使有效权重之和为 1.0
        Map<String, Double> finalWeight = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : validBase.entrySet()) {
            double w = entry.getValue();
            finalWeight.put(entry.getKey(), totalValidWeight == 0 ? 0 : w / totalValidWeight);
        }

        // ---- 3. 五个维度独立计算 ----
        SkillMatchResult reqResult  = calcSkillDimension(resumeSkillList, jdRequired, finalWeight.get("requiredSkill"),  "必备技能");
        SkillMatchResult prefResult = calcSkillDimension(resumeSkillList, jdPreferred, finalWeight.get("preferredSkill"), "加分技能");
        SkillMatchResult expResult  = calcNgramMatch(resumeExpText, jdExpReq, finalWeight.get("experience"),             "经验要求");
        SkillMatchResult eduResult  = calcEduMatch(resumeEdu, jdEduReq, finalWeight.get("education"),                   "学历要求");
        SkillMatchResult dutyResult = calcNgramMatch(resumeDutyTexts, jdDutyTexts, finalWeight.get("dutyCover"),         "职责覆盖");

        // ---- 3.5 经验维度补充判定 ----
        if (!jdExpReq.isEmpty()) {
            // 3.5a 年限数值对比：n-gram 未命中但简历年限达标 → 视为命中
            int jdMinMonths = extractMinRequiredMonths(jdExpReq);
            if (jdMinMonths > 0) {
                int resumeMaxMonths = extractMaxResumeMonths(resumeExpText);
                if (resumeMaxMonths >= jdMinMonths) {
                    expResult = reconcileExpWithDuration(expResult, jdExpReq,
                            finalWeight.get("experience"));
                }
            }
            // 3.5b "有项目经验" 等元问题：简历项目经历非空即视为命中
            boolean hasProjects = !resumeExpText.isEmpty();
            if (hasProjects) {
                expResult = reconcileExpWithMetaRequirement(expResult, jdExpReq,
                        finalWeight.get("experience"));
            }
            // 3.5c "应届生"：毕业年份 == 今年即视为应届
            boolean isFreshGrad = graduateYear == java.time.Year.now().getValue();
            if (isFreshGrad) {
                expResult = reconcileExpWithFreshGrad(expResult, jdExpReq,
                        finalWeight.get("experience"));
            }
        }

        // ---- 4. 汇总各维度明细 ----
        List<MatchDetails.MatchDimensionItem> dimList = List.of(
                reqResult.item, prefResult.item, expResult.item, eduResult.item, dutyResult.item);
        double totalRawScore = dimList.stream()
                .map(MatchDetails.MatchDimensionItem::dimScore)
                .reduce(0.0, Double::sum);

        // 一票否决 + 全空兜底
        int finalScore;
        if (totalValidWeight == 0) {
            finalScore = 100;
        } else if (reqResult.item.coverage() < 60) {
            finalScore = (int) Math.min(totalRawScore, 40);
        } else {
            finalScore = (int) Math.round(totalRawScore);
        }
        finalScore = Math.max(0, Math.min(100, finalScore));

        // ---- 5. 组装对外结果 ----
        MatchDetails details = new MatchDetails();

        // 五个维度的覆盖率
        details.setRequiredCoverage(jdRequired.isEmpty()  ? 100 : (int) Math.round(reqResult.item.coverage()));
        details.setPreferredCoverage(jdPreferred.isEmpty() ? 100 : (int) Math.round(prefResult.item.coverage()));
        details.setExperienceCoverage(jdExpReq.isEmpty()   ? 100 : (int) Math.round(expResult.item.coverage()));
        details.setEducationMatch(jdEduReq.isEmpty() || eduResult.item.coverage() >= 100);
        details.setResponsibilityCoverage(jdDutyTexts.isEmpty() ? 100 : (int) Math.round(dutyResult.item.coverage()));

        // 合并各维度的命中项、缺失项、不确定项（去重）
        List<String> allMatched = new ArrayList<>();
        allMatched.addAll(reqResult.matchedItems);
        allMatched.addAll(prefResult.matchedItems);
        allMatched.addAll(expResult.matchedItems);
        details.setMatchedItems(dedupe(allMatched));

        List<String> allMissing = new ArrayList<>();
        allMissing.addAll(reqResult.missingItems);
        allMissing.addAll(prefResult.missingItems);
        allMissing.addAll(expResult.missingItems);
        details.setMissingItems(dedupe(allMissing));

        details.setUnknownItems(dedupe(expResult.unknownItems));

        // 各维度得分 Map
        Map<String, Integer> dimScores = new LinkedHashMap<>();
        for (MatchDetails.MatchDimensionItem d : dimList) {
            dimScores.put(d.dimName(), (int) Math.round(d.dimScore()));
        }
        details.setDimensionScores(dimScores);

        // 技能别名映射（简历用词 ↔ JD 用词不一致时记录对应关系）
        Map<String, String> allAliases = new LinkedHashMap<>();
        allAliases.putAll(reqResult.aliasMatches);
        allAliases.putAll(prefResult.aliasMatches);
        details.setAliasMatches(allAliases.isEmpty() ? null : allAliases);

        // 算法内部字段（调试和分析用）
        details.setDimensionList(dimList);
        details.setTotalRawScore(totalRawScore);
        details.setMatchLevel(getMatchLevel(finalScore));
        details.setDynamicWeightMap(finalWeight);

        return new MatchResult(finalScore, details);
    }

    // ================================================================
    // 维度计算
    // ================================================================

    /** 技能维度匹配：技能标准化后计算覆盖率。 */
    private SkillMatchResult calcSkillDimension(
            List<String> resumeSkills, List<String> jdSkills, double weight, String dimName) {

        if (jdSkills.isEmpty()) {
            return SkillMatchResult.empty(dimName, weight);
        }

        Set<String> resumeSet = normalizeSkillSet(resumeSkills);

        Map<String, String> normalizedToOriginal = new LinkedHashMap<>();
        for (String s : resumeSkills) {
            normalizedToOriginal.putIfAbsent(normalizeSkill(s), s.trim());
        }

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Map<String, String> aliases = new LinkedHashMap<>();

        for (String jdSkill : jdSkills) {
            String norm = normalizeSkill(jdSkill);
            if (resumeSet.contains(norm)) {
                String original = normalizedToOriginal.getOrDefault(norm, norm);
                matched.add(jdSkill.trim());
                if (!original.equalsIgnoreCase(jdSkill.trim())) {
                    aliases.put(original, jdSkill.trim());
                }
            } else {
                missing.add(jdSkill.trim());
            }
        }

        long hit = matched.size();
        double coverage = hit * 100.0 / jdSkills.size();
        double dimScore = coverage * weight;

        MatchDetails.MatchDimensionItem item =
                new MatchDetails.MatchDimensionItem(dimName, weight, coverage, dimScore, hit);

        return new SkillMatchResult(item, matched, missing, Collections.emptyList(), aliases);
    }

    /** 基于字符级 bigram 重叠的文本维度匹配（经验 / 职责覆盖）。
     *
     * <p>对每个 JD 文本片段，在所有简历文本中寻找最佳 bigram 重叠比例。
     * 重叠比例 ≥ {@link #NGRAM_OVERLAP_THRESHOLD} 视为命中。
     * 未命中且含数字或"年"字的归入 unknownItems。
     *
     * @param resumeTexts 简历文本片段列表（每段是独立的标题或描述）
     * @param jdTexts     JD 要求文本片段列表
     * @param weight      该维度最终权重
     * @param dimName     维度名称
     * @return 匹配结果（hit/missing/unknown + 覆盖率）
     */
    private SkillMatchResult calcNgramMatch(
            List<String> resumeTexts, List<String> jdTexts,
            double weight, String dimName) {

        if (jdTexts.isEmpty()) {
            return SkillMatchResult.empty(dimName, weight);
        }

        // 预计算所有简历文本的 bigram 集合（避免重复生成）
        List<Set<String>> resumeBigramSets = new ArrayList<>();
        for (String text : resumeTexts) {
            Set<String> bigrams = generateBigrams(text);
            if (!bigrams.isEmpty()) {
                resumeBigramSets.add(bigrams);
            }
        }

        List<String> matched = new ArrayList<>();
        List<String> missing  = new ArrayList<>();
        List<String> unknown  = new ArrayList<>();

        for (String jdText : jdTexts) {
            Set<String> jdBigrams = generateBigrams(jdText);
            if (jdBigrams.isEmpty()) {
                // JD 文本过短（≤1 字符），无法做有意义的匹配
                unknown.add(jdText.trim());
                continue;
            }

            // 在所有简历文本中寻找最佳重叠
            double bestOverlap = 0.0;
            for (Set<String> resumeBigrams : resumeBigramSets) {
                long overlapCount = 0;
                for (String bg : jdBigrams) {
                    if (resumeBigrams.contains(bg)) {
                        overlapCount++;
                    }
                }
                double overlap = (double) overlapCount / jdBigrams.size();
                if (overlap > bestOverlap) {
                    bestOverlap = overlap;
                    if (bestOverlap >= NGRAM_OVERLAP_THRESHOLD) {
                        break; // 已达到阈值，无需继续搜索
                    }
                }
            }

            if (bestOverlap >= NGRAM_OVERLAP_THRESHOLD) {
                matched.add(jdText.trim());
            } else {
                // 未命中 n-gram 匹配，回退到缺失判定
                missing.add(jdText.trim());
            }
        }

        long hit = matched.size();
        double coverage = hit * 100.0 / jdTexts.size();
        double dimScore = coverage * weight;

        MatchDetails.MatchDimensionItem item =
                new MatchDetails.MatchDimensionItem(dimName, weight, coverage, dimScore, hit);

        return new SkillMatchResult(item, matched, missing, unknown, Collections.emptyMap());
    }

    /** 学历维度匹配。 */
    private SkillMatchResult calcEduMatch(
            String resumeEdu, List<String> jdEduList, double weight, String dimName) {

        if (jdEduList.isEmpty() || resumeEdu == null) {
            return SkillMatchResult.empty(dimName, weight);
        }
        Map<String, Integer> eduRank = Map.of(
                "大专", 1, "本科", 2, "硕士", 3, "博士", 4
        );

        int resumeRank = eduRank.getOrDefault(resumeEdu, 0);

        // 从 JD 学历文本中提取关键字再查等级，支持 "本科及以上" 等写法
        int minReqRank = jdEduList.stream()
                .map(this::extractDegreeKeyword)
                .filter(Objects::nonNull)
                .map(eduRank::get)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(0);

        double coverage = (resumeRank > 0 && minReqRank > 0 && resumeRank >= minReqRank) ? 100.0 : 0.0;

        if (minReqRank == 0 && resumeRank > 0) {
            coverage = 100.0;
        }

        double dimScore = coverage * weight;

        MatchDetails.MatchDimensionItem item =
                new MatchDetails.MatchDimensionItem(dimName, weight, coverage, dimScore,
                        coverage == 100 ? 1L : 0L);

        List<String> matched = coverage == 100 ? List.of(resumeEdu) : Collections.emptyList();
        List<String> missing = coverage == 0 ? new ArrayList<>(jdEduList) : Collections.emptyList();

        return new SkillMatchResult(item, matched, missing, Collections.emptyList(), Collections.emptyMap());
    }

    // ================================================================
    // 技能标准化与别名
    // ================================================================

    /**
     * 技能别名映射表：归一化后的键 → 标准名。
     * 人工维护，覆盖常见技术的不同写法、缩写、版本号变体。
     * <p>Map key 已经是去空格/连字符/小写后的形式，value 为标准统一名。
     */
    private static final Map<String, String> SKILL_ALIAS = Map.ofEntries(
            // ===== 语言 / 运行时 =====
            Map.entry("js",              "javascript"),
            Map.entry("ts",              "typescript"),
            Map.entry("golang",          "go"),
            Map.entry("cpp",             "c++"),
            Map.entry("cplusplus",       "c++"),
            Map.entry("csharp",          "c#"),
            Map.entry("dotnet",          ".net"),
            Map.entry("netcore",         ".net"),
            Map.entry("aspnet",          "asp.net"),
            Map.entry("vbnet",           "vb.net"),
            Map.entry("objectivec",      "objective-c"),
            Map.entry("objc",            "objective-c"),
            // ===== 前端框架 / 库 =====
            Map.entry("vuejs",           "vue"),
            Map.entry("vue2",            "vue"),
            Map.entry("vue3",            "vue"),
            Map.entry("react.js",        "react"),
            Map.entry("reactjs",         "react"),
            Map.entry("angularjs",       "angular"),
            Map.entry("angular.js",      "angular"),
            Map.entry("next.js",         "nextjs"),
            Map.entry("nuxt.js",         "nuxtjs"),
            Map.entry("jquery",          "jquery"),
            Map.entry("bootstrap3",      "bootstrap"),
            Map.entry("bootstrap4",      "bootstrap"),
            Map.entry("bootstrap5",      "bootstrap"),
            Map.entry("tailwindcss",     "tailwind"),
            Map.entry("sass",            "scss"),
            Map.entry("elementui",       "elementplus"),
            Map.entry("element",         "elementplus"),
            Map.entry("antd",            "antdesign"),
            Map.entry("antdesignvue",    "antdesign"),
            Map.entry("node.js",        "node"),
            Map.entry("nodejs",          "node"),
            // ===== 后端框架 =====
            Map.entry("springboot",      "springboot"),
            Map.entry("springmvc",       "springmvc"),
            Map.entry("springcloud",     "springcloud"),
            Map.entry("springsecurity",  "springsecurity"),
            Map.entry("mybatisplus",     "mybatis"),
            Map.entry("express.js",      "express"),
            Map.entry("expressjs",       "express"),
            Map.entry("nestjs",          "nestjs"),
            Map.entry("fastapi",         "fastapi"),
            // ===== 数据库 / 缓存 =====
            Map.entry("postgresql",      "postgres"),
            Map.entry("postgres",        "postgres"),
            Map.entry("mariadb",         "mysql"),
            Map.entry("mongodb",         "mongo"),
            Map.entry("elasticsearch",   "elasticsearch"),
            Map.entry("es",              "elasticsearch"),
            Map.entry("redis",           "redis"),
            Map.entry("memcached",       "memcached"),
            Map.entry("mssql",           "sqlserver"),
            Map.entry("sqlserver",       "sqlserver"),
            Map.entry("sqllite",         "sqlite"),
            // =====  DevOps / 云原生 =====
            Map.entry("k8s",             "kubernetes"),
            Map.entry("k3s",             "kubernetes"),
            Map.entry("gitlabci",        "gitlabci"),
            Map.entry("gitlab-ci",       "gitlabci"),
            Map.entry("githubactions",   "githubactions"),
            Map.entry("github-actions",  "githubactions"),
            Map.entry("cicd",            "ci/cd"),
            Map.entry("devops",          "devops"),
            Map.entry("aws",             "aws"),
            Map.entry("amazonwebservices","aws"),
            Map.entry("gcp",             "gcp"),
            Map.entry("googlecloud",     "gcp"),
            Map.entry("azure",           "azure"),
            Map.entry("alicloud",        "alicloud"),
            Map.entry("aliyun",          "alicloud"),
            Map.entry("terraform",       "terraform"),
            Map.entry("ansible",         "ansible"),
            Map.entry("prometheus",      "prometheus"),
            Map.entry("grafana",         "grafana"),
            // ===== 消息队列 / 流处理 =====
            Map.entry("rabbit",          "rabbitmq"),
            Map.entry("rocket",          "rocketmq"),
            Map.entry("apachekafka",     "kafka"),
            // ===== AI / 机器学习 =====
            Map.entry("sklearn",         "scikit-learn"),
            Map.entry("tensorflow2",     "tensorflow"),
            Map.entry("pytorch",         "pytorch"),
            Map.entry("opencv",          "opencv"),
            Map.entry("numpy",           "numpy"),
            Map.entry("pandas",          "pandas"),
            Map.entry("nlp",             "naturallanguageprocessing"),
            Map.entry("cv",              "computervision"),
            Map.entry("llm",             "largelanguagemodel"),
            // ===== 移动端 =====
            Map.entry("reactnative",     "reactnative"),
            Map.entry("react-native",    "reactnative"),
            Map.entry("rn",              "reactnative"),
            Map.entry("weex",            "weex"),
            Map.entry("uniapp",          "uniapp"),
            Map.entry("taro",            "taro"),
            // ===== 协议 / 标准 =====
            Map.entry("restful",         "rest"),
            Map.entry("restapi",         "rest"),
            Map.entry("graphql",         "graphql"),
            Map.entry("grpc",            "grpc"),
            Map.entry("websocket",       "websocket"),
            Map.entry("ws",              "websocket"),
            Map.entry("oauth",           "oauth2"),
            Map.entry("oauth2.0",        "oauth2"),
            Map.entry("jwt",             "jwt"),
            Map.entry("ssl",             "tls"),
            Map.entry("https",           "http"),
            // ===== 测试 =====
            Map.entry("junit5",          "junit"),
            Map.entry("unittest",        "unittest"),
            Map.entry("e2e",             "endtoendtest"),
            // ===== 构建 / 包管理 =====
            Map.entry("maven",           "maven"),
            Map.entry("gradle",          "gradle"),
            Map.entry("npm",             "npm"),
            Map.entry("yarn",            "yarn"),
            Map.entry("pnpm",            "pnpm"),
            Map.entry("webpack",         "webpack"),
            Map.entry("vite",            "vite"),
            // ===== Web 服务器 =====
            Map.entry("apachehttpserver","apache"),
            Map.entry("tomcat",          "tomcat"),
            Map.entry("jetty",           "jetty"),
            // ===== 操作系统 =====
            Map.entry("ubuntu",          "linux"),
            Map.entry("centos",          "linux"),
            Map.entry("debian",          "linux"),
            Map.entry("macos",           "macos"),
            Map.entry("osx",             "macos")
    );

    /**
     * 技能名称归一化：去空格、去连字符、小写，并映射常见别名。
     */
    private String normalizeSkill(String skill) {
        String stripped = skill.trim().toLowerCase().replaceAll("[-. ]", "");
        return SKILL_ALIAS.getOrDefault(stripped, stripped);
    }

    /** 批量标准化，返回 Set 用于快速 contains 查找。 */
    private Set<String> normalizeSkillSet(List<String> list) {
        return list.stream().map(this::normalizeSkill).collect(Collectors.toSet());
    }

    // ================================================================
    // 分词 & 等级判定
    // ================================================================

    /**
     * 从 JD 学历要求文本中提取关键字。
     * 按等级从高到低检查是否包含已知学历关键字，支持 "本科及以上"、"硕士以上" 等写法。
     *
     * <p>示例: "本科及以上" → "本科"；"硕士及以上学历" → "硕士"；"优秀院校优先" → null
     */
    private String extractDegreeKeyword(String eduText) {
        if (eduText == null || eduText.isBlank()) return null;
        // 按等级从高到低检查，优先匹配高水平学历
        for (String keyword : List.of("博士", "硕士", "本科", "大专")) {
            if (eduText.contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }

    // ================================================================
    // 经验年限提取与对比（支持年/月/日）
    // ================================================================

    /**
     * 从文本中解析年限，转换为月数。
     * 支持: "3年以上" → 36, "1-3年" → 12（取下限）, "5-10年" → 60,
     * "6个月" → 6, "30天" → 1, "应届生"/"不限经验" → 0。
     *
     * @return 提取到的月数，无法解析返回 0
     */
    private int parseExperienceMonths(String text) {
        if (text == null || text.isBlank()) return 0;
        // 不限经验 / 应届生
        if (text.contains("不限") || text.contains("应届") || text.contains("毕业生")) return 0;

        // 尝试匹配 "X-Y年" 或 "X-Y 年" 范围格式（取下限 X）
        java.util.regex.Matcher rangeMatcher = java.util.regex.Pattern.compile("(\\d+)\\s*[-~至到]\\s*(\\d+)\\s*年").matcher(text);
        if (rangeMatcher.find()) {
            return Integer.parseInt(rangeMatcher.group(1)) * 12;
        }

        // 尝试匹配 "X年以上" / "X年及以上" / "X年或以上" / "至少X年"
        java.util.regex.Matcher minMatcher = java.util.regex.Pattern.compile("(\\d+)\\s*年[以之]?[上内]|至少\\s*(\\d+)\\s*年").matcher(text);
        if (minMatcher.find()) {
            String num = minMatcher.group(1) != null ? minMatcher.group(1) : minMatcher.group(2);
            return Integer.parseInt(num) * 12;
        }

        // 尝试匹配 "X年"（独立年限）
        java.util.regex.Matcher yearMatcher = java.util.regex.Pattern.compile("(\\d+)\\s*年").matcher(text);
        if (yearMatcher.find()) {
            return Integer.parseInt(yearMatcher.group(1)) * 12;
        }

        // 尝试匹配 "X个月"
        java.util.regex.Matcher monthMatcher = java.util.regex.Pattern.compile("(\\d+)\\s*个?月").matcher(text);
        if (monthMatcher.find()) {
            return Integer.parseInt(monthMatcher.group(1));
        }

        // 尝试匹配 "X天"
        java.util.regex.Matcher dayMatcher = java.util.regex.Pattern.compile("(\\d+)\\s*天").matcher(text);
        if (dayMatcher.find()) {
            return Integer.parseInt(dayMatcher.group(1)) / 30; // 粗略换算
        }

        return 0;
    }

    /**
     * 将含年限要求的 JD 条目从未命中修正为命中（简历年限达标时）。
     * 重新计算覆盖率与维度得分。
     */
    private SkillMatchResult reconcileExpWithDuration(
            SkillMatchResult ngramResult, List<String> jdExpReqs,
            double weight) {

        List<String> matched = new ArrayList<>(ngramResult.matchedItems);
        List<String> missing = new ArrayList<>(ngramResult.missingItems);

        for (String req : jdExpReqs) {
            if (missing.contains(req.trim()) && parseExperienceMonths(req) > 0) {
                // n-gram 未命中但含明确年限，且简历年限达标 → 修正为命中
                missing.remove(req.trim());
                matched.add(req.trim());
            }
        }

        long hit = matched.size();
        double coverage = hit * 100.0 / jdExpReqs.size();
        double dimScore = coverage * weight;

        MatchDetails.MatchDimensionItem item =
                new MatchDetails.MatchDimensionItem("经验要求", weight, coverage, dimScore, hit);

        return new SkillMatchResult(item, matched, missing,
                new ArrayList<>(ngramResult.unknownItems), Collections.emptyMap());
    }

    /**
     * 将"有项目经验"等元问题未命中项修正为命中（简历项目经历非空即满足）。
     */
    private SkillMatchResult reconcileExpWithMetaRequirement(
            SkillMatchResult ngramResult, List<String> jdExpReqs,
            double weight) {

        List<String> matched = new ArrayList<>(ngramResult.matchedItems);
        List<String> missing = new ArrayList<>(ngramResult.missingItems);

        for (String req : jdExpReqs) {
            if (missing.contains(req.trim()) && isMetaExperienceRequirement(req)) {
                missing.remove(req.trim());
                matched.add(req.trim());
            }
        }

        long hit = matched.size();
        double coverage = hit * 100.0 / jdExpReqs.size();
        double dimScore = coverage * weight;

        MatchDetails.MatchDimensionItem item =
                new MatchDetails.MatchDimensionItem("经验要求", weight, coverage, dimScore, hit);

        return new SkillMatchResult(item, matched, missing,
                new ArrayList<>(ngramResult.unknownItems), Collections.emptyMap());
    }

    /**
     * 将"应届生"类 JD 要求从未命中修正为命中（毕业年份 == 今年即满足）。
     */
    private SkillMatchResult reconcileExpWithFreshGrad(
            SkillMatchResult ngramResult, List<String> jdExpReqs,
            double weight) {

        List<String> matched = new ArrayList<>(ngramResult.matchedItems);
        List<String> missing = new ArrayList<>(ngramResult.missingItems);

        for (String req : jdExpReqs) {
            if (missing.contains(req.trim()) && req.contains("应届")) {
                missing.remove(req.trim());
                matched.add(req.trim());
            }
        }

        long hit = matched.size();
        double coverage = hit * 100.0 / jdExpReqs.size();
        double dimScore = coverage * weight;

        MatchDetails.MatchDimensionItem item =
                new MatchDetails.MatchDimensionItem("经验要求", weight, coverage, dimScore, hit);

        return new SkillMatchResult(item, matched, missing,
                new ArrayList<>(ngramResult.unknownItems), Collections.emptyMap());
    }

    /** 判断是否为"有项目经验"类元问题——问的是有没有经历，不是具体技术关键词。 */
    private boolean isMetaExperienceRequirement(String text) {
        if (text == null || text.isBlank()) return false;
        // 匹配: "有项目经验", "具备项目经验", "有实习经验", "有工作经验", "有项目经历" 等
        return text.matches(".*(有|具备|具有).*(项目经验|实习经验|工作经验|项目经历|开发经验).*");
    }

    /** 从 JD 经验要求列表中提取最低年限要求（月数），取所有要求的最大值。 */
    private int extractMinRequiredMonths(List<String> jdExpReqs) {
        return jdExpReqs.stream()
                .mapToInt(this::parseExperienceMonths)
                .max()
                .orElse(0);
    }

    /** 从简历文本列表中提取最大经验年限（月数），取所有文本中的最大值。 */
    private int extractMaxResumeMonths(List<String> resumeTexts) {
        return resumeTexts.stream()
                .mapToInt(this::parseExperienceMonths)
                .max()
                .orElse(0);
    }

    /**
     * 生成字符级 bigram 集合。
     * 预处理：去除所有空白字符（空格、tab、换行等），然后生成所有相邻 2-字符片段。
     * 输入长度 ≤ 1 时返回空集合。
     *
     * <p>示例: "微服务" → {"微服", "服务"}；"AB" → {"AB"}；"A" → {}
     */
    private Set<String> generateBigrams(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        String cleaned = text.replaceAll("\\s+", "");
        if (cleaned.length() < 2) return Collections.emptySet();
        Set<String> result = new LinkedHashSet<>();
        for (int i = 0; i < cleaned.length() - 1; i++) {
            result.add(cleaned.substring(i, i + 2));
        }
        return result;
    }

    /** 根据分数判定匹配等级。 */
    private String getMatchLevel(int score) {
        if (score >= 80) return "高匹配";
        if (score >= 60) return "中等匹配";
        if (score >= 40) return "低匹配";
        return "不匹配";
    }

    // ================================================================
    // 内部工具方法
    // ================================================================

    /** null 安全：将 null 列表转为空列表。 */
    private static <T> List<T> orEmpty(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }

    /** 列表去重，保持原始顺序。 */
    private static List<String> dedupe(List<String> list) {
        return list.stream().distinct().collect(Collectors.toList());
    }

    // ================================================================
    // 内部类型
    // ================================================================

    /**
     * 单个维度的匹配结果。
     */
    static class SkillMatchResult {
        final MatchDetails.MatchDimensionItem item;
        final List<String> matchedItems;
        final List<String> missingItems;
        final List<String> unknownItems;
        final Map<String, String> aliasMatches;

        SkillMatchResult(MatchDetails.MatchDimensionItem item,
                         List<String> matchedItems, List<String> missingItems,
                         List<String> unknownItems, Map<String, String> aliasMatches) {
            this.item = item;
            this.matchedItems = Collections.unmodifiableList(matchedItems);
            this.missingItems = Collections.unmodifiableList(missingItems);
            this.unknownItems = Collections.unmodifiableList(unknownItems);
            this.aliasMatches = Collections.unmodifiableMap(aliasMatches);
        }

        static SkillMatchResult empty(String dimName, double weight) {
            MatchDetails.MatchDimensionItem emptyItem =
                    new MatchDetails.MatchDimensionItem(dimName, weight, 0.0, 0.0, 0L);
            return new SkillMatchResult(emptyItem,
                    Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyMap());
        }
    }

    /** 算法执行结果：最终分数 + 完整明细。 */
    public record MatchResult(int matchScore, MatchDetails details) {
    }
}
