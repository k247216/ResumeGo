package com.resumego.matching.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Map;

/**
 * 匹配维度详情。
 * 字段设计见 harness-docs/testing/match-test-cases.md 附录 B。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "requiredCoverage", "preferredCoverage", "experienceCoverage",
        "educationMatch", "responsibilityCoverage",
        "matchedItems", "missingItems", "unknownItems",
        "dimensionScores", "aliasMatches",
        "dimensionList", "totalRawScore", "matchLevel", "dynamicWeightMap"
})
public class MatchDetails {

    // ============================================================
    // 对外展示字段（设计文档附录 B）
    // ============================================================

    /** 必备技能覆盖率 0-100 */
    @JsonProperty("requiredCoverage")
    private int requiredCoverage;

    /** 加分技能覆盖率 0-100 */
    @JsonProperty("preferredCoverage")
    private int preferredCoverage;

    /** 经验要求覆盖率 0-100 */
    @JsonProperty("experienceCoverage")
    private int experienceCoverage;

    /** 教育背景是否匹配 */
    @JsonProperty("educationMatch")
    private boolean educationMatch;

    /** 职责关键词覆盖率 0-100（参考维度） */
    @JsonProperty("responsibilityCoverage")
    private int responsibilityCoverage;

    /** 已覆盖的技能/经验项 */
    @JsonProperty("matchedItems")
    private List<String> matchedItems;

    /** 缺失的必备技能/经验项 */
    @JsonProperty("missingItems")
    private List<String> missingItems;

    /** 无法判定的项 */
    @JsonProperty("unknownItems")
    private List<String> unknownItems;

    /** 各维度得分明细 */
    @JsonProperty("dimensionScores")
    private Map<String, Integer> dimensionScores;

    /** 技能别名匹配记录：简历技能名 → JD 技能名 */
    @JsonProperty("aliasMatches")
    private Map<String, String> aliasMatches;

    // ============================================================
    // 算法内部字段（供调试/日志，可选输出）
    // ============================================================

    /** 各维度计算明细 */
    @JsonProperty("dimensionList")
    private List<MatchDimensionItem> dimensionList;

    /** 加权前原始总分 */
    @JsonProperty("totalRawScore")
    private double totalRawScore;

    /** 匹配等级：高匹配 / 中等匹配 / 低匹配 / 不匹配 */
    @JsonProperty("matchLevel")
    private String matchLevel;

    /** 动态调整后的权重映射 */
    @JsonProperty("dynamicWeightMap")
    private Map<String, Double> dynamicWeightMap;

    // ============================================================
    // 维度明细项
    // ============================================================

    /**
     * 单个匹配维度的计算结果。
     */
    public record MatchDimensionItem(
            @JsonProperty("dimName") String dimName,
            @JsonProperty("weight") double weight,
            @JsonProperty("coverage") double coverage,
            @JsonProperty("dimScore") double dimScore,
            @JsonProperty("hitCount") long hitCount
    ) {
    }

    // ============================================================
    // builder-style setters
    // ============================================================

    public int getRequiredCoverage() { return requiredCoverage; }
    public MatchDetails setRequiredCoverage(int v) { this.requiredCoverage = v; return this; }

    public int getPreferredCoverage() { return preferredCoverage; }
    public MatchDetails setPreferredCoverage(int v) { this.preferredCoverage = v; return this; }

    public int getExperienceCoverage() { return experienceCoverage; }
    public MatchDetails setExperienceCoverage(int v) { this.experienceCoverage = v; return this; }

    public boolean isEducationMatch() { return educationMatch; }
    public MatchDetails setEducationMatch(boolean v) { this.educationMatch = v; return this; }

    public int getResponsibilityCoverage() { return responsibilityCoverage; }
    public MatchDetails setResponsibilityCoverage(int v) { this.responsibilityCoverage = v; return this; }

    public List<String> getMatchedItems() { return matchedItems; }
    public MatchDetails setMatchedItems(List<String> v) { this.matchedItems = v; return this; }

    public List<String> getMissingItems() { return missingItems; }
    public MatchDetails setMissingItems(List<String> v) { this.missingItems = v; return this; }

    public List<String> getUnknownItems() { return unknownItems; }
    public MatchDetails setUnknownItems(List<String> v) { this.unknownItems = v; return this; }

    public Map<String, Integer> getDimensionScores() { return dimensionScores; }
    public MatchDetails setDimensionScores(Map<String, Integer> v) { this.dimensionScores = v; return this; }

    public Map<String, String> getAliasMatches() { return aliasMatches; }
    public MatchDetails setAliasMatches(Map<String, String> v) { this.aliasMatches = v; return this; }

    public List<MatchDimensionItem> getDimensionList() { return dimensionList; }
    public MatchDetails setDimensionList(List<MatchDimensionItem> v) { this.dimensionList = v; return this; }

    public double getTotalRawScore() { return totalRawScore; }
    public MatchDetails setTotalRawScore(double v) { this.totalRawScore = v; return this; }

    public String getMatchLevel() { return matchLevel; }
    public MatchDetails setMatchLevel(String v) { this.matchLevel = v; return this; }

    public Map<String, Double> getDynamicWeightMap() { return dynamicWeightMap; }
    public MatchDetails setDynamicWeightMap(Map<String, Double> v) { this.dynamicWeightMap = v; return this; }
}
