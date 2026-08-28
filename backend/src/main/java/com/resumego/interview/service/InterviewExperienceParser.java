package com.resumego.interview.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把知识库中的真实面经笔记转换为可练习题集。
 *
 * 只识别用户明确写出的列表项，不调用 AI、不补写题目；公司/岗位/图标也只接受
 * Markdown front matter 中的显式字段。无法确定边界时直接拒绝，避免把说明文字伪装成面经。
 */
public class InterviewExperienceParser {

    private static final Pattern FRONT_MATTER = Pattern.compile("(?s)^\\s*---\\s*\\R(.*?)\\R---\\s*(.*)$");
    private static final Pattern QUESTION_LINE = Pattern.compile("^\\s*(?:(?:\\d+)[.)、]|[-*])\\s+(.+?)\\s*$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final int MAX_METADATA_LENGTH = 120;

    public record Parsed(String companyName, String targetRole, String companyIconKey, List<String> questions) {
    }

    public Parsed parse(String title, String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalArgumentException("面经资料没有可识别的题目");
        }
        String content = rawContent.replace("\r\n", "\n").replace('\r', '\n');
        Metadata metadata = parseMetadata(content);
        String body = metadata.body();
        List<String> questions = new ArrayList<>();
        for (String line : body.split("\\n")) {
            Matcher matcher = QUESTION_LINE.matcher(line);
            if (matcher.matches()) {
                String question = matcher.group(1).trim();
                if (!question.isEmpty()) questions.add(question);
            }
        }
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("请用序号或列表符号明确列出真实面经题目");
        }
        return new Parsed(metadata.companyName(), metadata.targetRole(), metadata.companyIconKey(), List.copyOf(questions));
    }

    private Metadata parseMetadata(String content) {
        Matcher matcher = FRONT_MATTER.matcher(content);
        if (!matcher.matches()) return new Metadata(null, null, null, content);
        String frontMatter = matcher.group(1);
        String body = matcher.group(2);
        String company = null;
        String role = null;
        String icon = null;
        for (String line : frontMatter.split("\\n")) {
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            String key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();
            if (value.isEmpty()) continue;
            switch (key) {
                case "company", "companyname" -> company = validateMetadata(value, "公司名称");
                case "role", "targetrole" -> role = validateMetadata(value, "目标岗位");
                case "icon", "companyiconkey" -> icon = validateMetadata(value, "公司图标标识");
                default -> { /* 允许用户在 front matter 中保留其他字段，但不消费它们。 */ }
            }
        }
        return new Metadata(company, role, icon, body);
    }

    private String validateMetadata(String value, String label) {
        if (value.length() > MAX_METADATA_LENGTH) {
            throw new IllegalArgumentException(label + "过长，最大 " + MAX_METADATA_LENGTH + " 字符");
        }
        return value;
    }

    private record Metadata(String companyName, String targetRole, String companyIconKey, String body) {
    }
}
