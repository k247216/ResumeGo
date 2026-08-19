package com.resumego.optimization;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 优化建议模块入参校验工具。
 * 集中管理所有校验规则和常量，确保校验逻辑不被分散。
 */
public final class SuggestionValidation {

    private SuggestionValidation() {
        // 工具类，禁止实例化
    }

    // ── 校验常量 ──

    /** 单次 AI 生成建议数量上限 */
    public static final int MAX_SUGGESTIONS = 50;

    /** sectionKey 最大长度 */
    public static final int MAX_SECTION_KEY_LENGTH = 200;

    /** originalText/suggestedText 最大长度 */
    public static final int MAX_TEXT_LENGTH = 5000;

    /** reason/targetRequirement 最大长度 */
    public static final int MAX_REASON_LENGTH = 2000;

    /** confidence 合法取值 */
    public static final Set<String> VALID_CONFIDENCE_VALUES = Set.of("high", "medium", "low");

    /** sectionKey 合法格式：字母开头，后跟字母数字下划线方括号点的组合 */
    private static final Pattern SECTION_KEY_PATTERN =
            Pattern.compile("^[a-zA-Z][a-zA-Z0-9_\\[\\].]*$");

    /** 提取关键词（数字、技术名词）的正则 */
    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z0-9]+(-[a-zA-Z0-9]+)*");

    // ── 校验方法 ──

    /**
     * 校验 ID 必须为正整数。
     */
    public static void requirePositive(long id, String fieldName) {
        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " 必须为正整数，实际值: " + id);
        }
    }

    /**
     * 校验 sectionKey 格式和长度。
     */
    public static void validateSectionKey(String sectionKey) {
        if (sectionKey == null || sectionKey.isBlank()) {
            throw new IllegalArgumentException("sectionKey 不能为空");
        }
        if (sectionKey.length() > MAX_SECTION_KEY_LENGTH) {
            throw new IllegalArgumentException("sectionKey 长度不能超过 " + MAX_SECTION_KEY_LENGTH);
        }
        if (!SECTION_KEY_PATTERN.matcher(sectionKey).matches()) {
            throw new IllegalArgumentException(
                    "sectionKey 格式不合法: " + sectionKey + "，必须为字母开头的标识符");
        }
    }

    /**
     * 校验文本字段非空且长度合规。
     */
    public static void validateTextField(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " 长度不能超过 " + maxLength + "，实际: " + value.length());
        }
    }

    /**
     * 校验 confidence 取值。
     */
    public static void validateConfidence(String confidence) {
        if (confidence == null || !VALID_CONFIDENCE_VALUES.contains(confidence)) {
            throw new IllegalArgumentException(
                    "confidence 必须为 high/medium/low 之一，实际值: " + confidence);
        }
    }

    /**
     * 校验建议数量上限。
     */
    public static void validateSuggestionsCount(int count) {
        if (count > MAX_SUGGESTIONS) {
            throw new IllegalArgumentException(
                    "AI 返回的建议数量超出上限: " + count + " > " + MAX_SUGGESTIONS);
        }
    }

    // ── 编造事实检测 ──

    /** 红牌关键词：数字（≥100）、奖项、证书、知名公司 — 出现这些才可能被标 high_risk */
    private static final Set<String> RED_FLAG_WORDS = Set.of(
            "奖", "金奖", "银奖", "铜奖", "一等奖", "二等奖", "三等奖",
            "国家级", "省级", "市级", "国际", "全国",
            "证书", "认证", "执照", "专利",
            "阿里巴巴", "阿里", "腾讯", "字节跳动", "华为", "百度", "京东", "美团",
            "google", "microsoft", "amazon", "apple", "meta", "facebook",
            "award", "certificate", "certification", "patent"
    );

    /** 大红数字：≥100 的整数通常暗示具体成就数据 */
    private static final Pattern BIG_NUMBER = Pattern.compile("\\b\\d{3,}\\b");

    /** 新词占比阈值：suggestedText 中超过此比例的关键词不在证据中，才触发 high_risk */
    private static final double FABRICATION_RATIO = 0.40;

    /**
     * 检测 suggestedText 是否包含编造的重大事实（红牌检测）。
     * <p>
     * 新版策略：不再因为任何新词就标记 high_risk，而是检测两类红牌信号：
     * <ol>
     *   <li>大数字（≥100）</li>
     *   <li>奖项/证书/知名公司名称</li>
     *   <li>以上信号 + 新词占比 > 40%</li>
     * </ol>
     * 仅当红牌信号命中，或新词占比过高时，才返回 true。
     *
     * @param suggestedText  AI 建议的修改文本
     * @param evidenceWords  证据中的关键词集合
     * @return true 表示检测到疑似编造（应标记 high_risk）
     */
    public static boolean detectFabrication(String suggestedText, Set<String> evidenceWords) {
        if (suggestedText == null || suggestedText.isBlank()) {
            return false;
        }
        if (evidenceWords == null || evidenceWords.isEmpty()) {
            return false;
        }

        Set<String> suggestionWords = extractKeywords(suggestedText);

        // 计算新词占比
        Set<String> newWords = new HashSet<>(suggestionWords);
        newWords.removeAll(evidenceWords);

        if (newWords.isEmpty()) {
            return false; // 全部匹配，安全
        }

        double ratio = suggestionWords.isEmpty()
                ? 0 : (double) newWords.size() / suggestionWords.size();

        // 检查红牌信号：大数字或奖项/证书/知名公司
        boolean hasRedFlag = false;
        for (String word : newWords) {
            if (RED_FLAG_WORDS.contains(word) || BIG_NUMBER.matcher(word).matches()) {
                hasRedFlag = true;
                break;
            }
        }

        // 仅当有红牌信号 + 一定新词比例，或新词占比极高时，才标记 high_risk
        if (hasRedFlag && ratio >= 0.10) {
            return true;
        }
        if (ratio >= FABRICATION_RATIO) {
            return true;
        }

        return false;
    }

    /**
     * 从证据文本中提取关键词集合（技术名词、数字）。
     *
     * @param actionText 证据行动文本
     * @param resultText 证据结果文本
     * @param skillTags  证据技能标签
     * @return 关键词集合（小写）
     */
    public static Set<String> extractEvidenceKeywords(String actionText, String resultText,
                                                       List<String> skillTags) {
        Set<String> keywords = new HashSet<>();

        if (actionText != null) {
            keywords.addAll(extractKeywords(actionText));
        }
        if (resultText != null) {
            keywords.addAll(extractKeywords(resultText));
        }
        if (skillTags != null) {
            for (String tag : skillTags) {
                keywords.addAll(extractKeywords(tag));
            }
        }

        return keywords;
    }

    /**
     * 从文本中提取关键词（长度 >= 2 的字母数字组合，忽略常见停用词）。
     */
    private static Set<String> extractKeywords(String text) {
        Set<String> result = new HashSet<>();
        if (text == null || text.isBlank()) {
            return result;
        }

        var matcher = WORD_PATTERN.matcher(text.toLowerCase());
        while (matcher.find()) {
            String word = matcher.group();
            if (word.length() >= 2 && !isStopWord(word)) {
                result.add(word);
            }
        }
        return result;
    }

    private static boolean isStopWord(String word) {
        return switch (word) {
            case "的", "了", "是", "在", "和", "与", "或", "对", "为", "及",
                 "the", "and", "for", "are", "was", "has", "had", "not", "but",
                 "its", "his", "her", "our", "their", "this", "that", "with",
                 "from", "have", "been", "can", "will", "would", "could", "should",
                 "into", "also", "such", "than", "then", "now", "just", "very",
                 "each", "all", "any", "some", "most", "more", "only", "other",
                 "new", "good", "high", "low", "well", "much", "many", "few",
                 "one", "two", "three", "get", "got", "set", "put", "use", "used",
                 "using", "make", "made", "per", "day", "year", "month", "week" -> true;
            default -> false;
        };
    }
}