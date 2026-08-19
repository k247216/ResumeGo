package com.resumego.ai;

import com.resumego.job.dto.ParsedJobDescriptionDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * JD 解析 Stub 实现。
 * 使用关键词和规则匹配抽取结构化字段，不依赖 AI 调用。
 * S1-08 替换为千问 Max 真实调用后作为降级兜底。
 */
@Service
public class StubJdParseService implements JdParseService {

    // ── 技能关键词（按长度降序，避免短词误匹配） ──
    private static final List<String> TECH_SKILLS = Arrays.asList(
            "Spring Boot", "Spring Cloud", "Spring MVC", "Spring",
            "MyBatis-Plus", "MyBatis", "Hibernate", "JPA",
            "RESTful API", "REST API",
            "Node.js", "Express", "Koa",
            "TypeScript", "JavaScript", "ES6",
            "Vue.js", "Vue", "React", "Angular",
            "Django", "Flask", "FastAPI", "Tornado",
            "PostgreSQL", "MySQL", "MongoDB", "Redis", "Oracle", "SQLite", "Elasticsearch",
            "Docker", "Kubernetes", "K8s", "Jenkins", "GitLab CI", "GitHub Actions",
            "GraphQL", "gRPC", "WebSocket", "MQTT",
            "Linux", "Shell", "Nginx", "Tomcat", "Apache",
            "微服务", "分布式", "高并发", "多线程", "消息队列",
            "Java", "Python", "Go", "C++", "Rust", "PHP", "Ruby", "Kotlin", "Scala",
            "Git", "SVN",
            "HTML5", "CSS3", "Sass", "Less", "Webpack", "Vite"
    );

    static {
        // 按长度降序排列，确保长词优先匹配
        TECH_SKILLS.sort(Comparator.comparingInt(String::length).reversed());
    }

    // ── 预编译正则 ──
    private static final Pattern RESP_PATTERN = Pattern.compile(
            "(负责|参与|完成|设计|开发|编写|维护|优化|分析|调研|支持|协助|主导|推进|落地)"
                    + "([^。；;，,\\n]{4,80})"
    );

    private static final Pattern EXP_PATTERN = Pattern.compile(
            "(\\d+年[以之]?[上内]?[^，]{0,50}(?:经验|开发经验|工作经验|开发|工作)|"
                    + "有[^，]{0,50}(?:项目经验|实习经验|工作经验)|"
                    + "具备[^，]{0,50}(?:项目经验|开发经验))"
    );

    private static final Pattern EDU_PATTERN = Pattern.compile(
            "(本科|硕士|博士|研究生|大专|学士|"
                    + "计算机(?:类|相关)?专业|软件工程|通信工程|电子信息|信息工程|网络工程|"
                    + "不限学历|学历不限|专业不限|"
                    + "计算机专业优先|计算机相关专业优先|计算机类专业优先|"
                    + "软件工程专业优先|通信工程专业优先|电子信息专业优先)"
    );

    // 加分技能标记词
    private static final List<String> BONUS_MARKERS = Arrays.asList(
            "优先", "加分", "plus", "bonus", "nice to have", "熟悉.*优先"
    );

    // 加分技能上下文提取半径（前后各 N 字符）
    private static final int BONUS_CTX_RADIUS = 30;

    // ── 公共入口 ──

    @Override
    public ParsedJobDescriptionDTO parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return emptyResult();
        }

        // 预处理：合并多余空白、统一标点
        String text = normalize(rawText);

        ParsedJobDescriptionDTO result = new ParsedJobDescriptionDTO();
        result.setRequiredSkills(extractSkills(text));
        result.setPreferredSkills(extractPreferredSkills(text));
        result.setResponsibilities(extractResponsibilities(text));
        result.setExperienceRequirements(extractExperienceRequirements(text));
        result.setEducationRequirements(extractEducationRequirements(text));

        return result;
    }

    // ── 文本预处理 ──

    private String normalize(String raw) {
        return raw
                .replaceAll("[ 　\t]+", " ")     // 合并空白
                .replaceAll("\\n{3,}", "\n\n")    // 合并多余空行
                .replaceAll("[：:]\\s*", "：")      // 统一冒号
                .trim();
    }

    // ── 技能提取 ──

    private List<String> extractSkills(String text) {
        LinkedHashSet<String> found = new LinkedHashSet<>();
        String lower = text.toLowerCase();

        for (String skill : TECH_SKILLS) {
            if (lower.contains(skill.toLowerCase())) {
                found.add(skill);
            }
        }
        return new ArrayList<>(found);
    }

    // ── 加分技能提取 ──

    private static final Pattern BONUS_SENTENCE = Pattern.compile(
            "[^。；\\n;]*?(?:优先|加分|plus|bonus|nice to have)[^。；\\n;]*[。；\\n;]?",
            Pattern.CASE_INSENSITIVE
    );

    private List<String> extractPreferredSkills(String text) {
        LinkedHashSet<String> result = new LinkedHashSet<>();

        // 找到所有包含加分标记的句子
        Matcher m = BONUS_SENTENCE.matcher(text);
        while (m.find()) {
            String sentence = m.group().replaceAll("[。；\\n;]$", "").trim();
            if (sentence.length() < 4) continue;

            // 尝试从句子中抽取技能关键词
            List<String> skillsInSentence = new ArrayList<>();
            for (String skill : TECH_SKILLS) {
                if (sentence.toLowerCase().contains(skill.toLowerCase()) && skill.length() > 2) {
                    skillsInSentence.add(skill);
                }
            }

            if (!skillsInSentence.isEmpty()) {
                result.addAll(skillsInSentence);
            } else if (sentence.length() >= 4) {
                // 没有匹配到具体技能时，保留原文描述
                result.add(sentence);
            }
        }

        // 回退：直接用独立关键词匹配
        if (result.isEmpty()) {
            for (String kw : Arrays.asList("优先", "加分")) {
                int idx = text.indexOf(kw);
                if (idx >= 0) {
                    int start = Math.max(0, idx - 20);
                    int end = Math.min(text.length(), idx + kw.length() + 20);
                    String fallback = text.substring(start, end).trim();
                    if (fallback.length() >= 4) {
                        result.add(fallback);
                    }
                }
            }
        }

        return new ArrayList<>(result);
    }

    // ── 职责提取 ──

    private List<String> extractResponsibilities(String text) {
        LinkedHashSet<String> result = new LinkedHashSet<>();

        // 方式 1：先按标点拆分为短句，再逐句匹配职责关键词，提升分行粒度
        String[] clauses = text.split("[，；。\\n]");
        for (String clause : clauses) {
            if (clause.isBlank()) continue;
            Matcher m = RESP_PATTERN.matcher(clause);
            if (m.find()) {
                String resp = (m.group(1) + m.group(2)).trim();
                if (resp.length() >= 4) {
                    result.add(cleanSegment(resp));
                }
            }
        }

        // 方式 2：按序号列表拆分（"1. xxx / 2. xxx"），补齐语句边界
        Pattern listPattern = Pattern.compile("(?:^|[\\n])\\s*(?:\\d+[.、)]|[-•·])\\s*([^\\n。；;，]{4,100})");
        Matcher listMatcher = listPattern.matcher(text);
        while (listMatcher.find()) {
            String item = listMatcher.group(1).trim();
            if (item.length() >= 4) {
                result.add(cleanSegment(item));
            }
        }

        return new ArrayList<>(result);
    }

    // ── 经验要求提取 ──

    private static final Pattern EDU_EXCLUDE = Pattern.compile(
            "本科|硕士|博士|研究生|大专|学士|学历|毕业证|学位证|\\d{2}学历"
                    + "|计算机专业|软件工程|通信工程|电子信息|信息工程|网络工程"
                    + "|专业不限|专业优先|相关专业",
            Pattern.CASE_INSENSITIVE
    );

    private List<String> extractExperienceRequirements(String text) {
        LinkedHashSet<String> result = new LinkedHashSet<>();

        // 先按标点拆分为短句，再逐句匹配，提升分行粒度并防止跨句污染
        String[] clauses = text.split("[，；。\\n]");
        for (String clause : clauses) {
            if (clause.isBlank()) continue;
            // 跳过纯学历短句（不含年限+经验关键词的学历描述）
            if (EDU_EXCLUDE.matcher(clause).find()
                    && !clause.matches(".*\\d+年.*(?:经验|开发|工作).*")) {
                continue;
            }

            Matcher m = EXP_PATTERN.matcher(clause);
            while (m.find()) {
                String exp = m.group().trim();
                if (exp.length() >= 3 && !result.contains(exp) && !EDU_EXCLUDE.matcher(exp).find()) {
                    result.add(cleanSegment(exp));
                }
            }
        }

        // 额外检测应届/校招含义的经验描述
        if (text.contains("应届") || text.contains("校招") || text.contains("毕业")) {
            result.add("面向应届毕业生");
        }

        return new ArrayList<>(result);
    }

    // ── 学历要求提取 ──

    private List<String> extractEducationRequirements(String text) {
        LinkedHashSet<String> result = new LinkedHashSet<>();

        Matcher m = EDU_PATTERN.matcher(text);
        while (m.find()) {
            String edu = m.group().trim();
            if (!result.contains(edu)) {
                result.add(edu);
            }
        }

        return new ArrayList<>(result);
    }

    // ── 工具方法 ──

    /** 清理文本片段末尾的残缺标点 */
    private String cleanSegment(String s) {
        return s.replaceAll("[，,。；;、\\s]+$", "").trim();
    }

    private ParsedJobDescriptionDTO emptyResult() {
        ParsedJobDescriptionDTO empty = new ParsedJobDescriptionDTO();
        empty.setRequiredSkills(List.of());
        empty.setPreferredSkills(List.of());
        empty.setResponsibilities(List.of());
        empty.setExperienceRequirements(List.of());
        empty.setEducationRequirements(List.of());
        return empty;
    }
}
