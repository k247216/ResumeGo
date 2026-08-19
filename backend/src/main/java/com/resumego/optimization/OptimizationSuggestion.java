package com.resumego.optimization;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 优化建议实体。
 * 对应 optimization_suggestions 表，记录每条 AI 生成的简历修改建议。
 * 一条建议对应一个可独立采纳或拒绝的修改位置。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@TableName("optimization_suggestions")
public class OptimizationSuggestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的匹配结果 ID
     */
    @TableField("job_match_id")
    private Long jobMatchId;

    /**
     * 适用的简历版本 ID
     */
    @TableField("resume_version_id")
    private Long resumeVersionId;

    /**
     * 来源能力证据 ID（可为空，无证据时为 null）
     */
    @TableField("evidence_id")
    private Long evidenceId;

    /**
     * 建议修改的简历节点标识（如 "projects[0]", "skills"）
     */
    @TableField("section_key")
    private String sectionKey;

    /**
     * 当前简历中的原文
     */
    @TableField("original_text")
    private String originalText;

    /**
     * AI 建议修改后的文本（无证据时为 null）
     */
    @TableField("suggested_text")
    private String suggestedText;

    /**
     * 修改原因说明（引用 JD 要求或评分扣分项）
     */
    @TableField("reason_text")
    private String reasonText;

    /**
     * 对应 JD 的具体要求原文
     */
    @TableField("target_requirement")
    private String targetRequirement;

    /**
     * 建议状态：pending / accepted / rejected / evidence_required / high_risk
     */
    @TableField("status")
    private String status;

    /**
     * 编造风险等级：high / low / null（null 表示未检测）
     */
    @TableField("risk_level")
    private String riskLevel;

    /**
     * Prompt 版本号
     */
    @TableField("prompt_version")
    private String promptVersion;

    /**
     * 生成此建议对应的 AI 调用审计 ID
     */
    @TableField("generation_ai_invocation_id")
    private Long generationAiInvocationId;

    /**
     * 采纳后产生的新简历版本 ID（未采纳时为 null）
     */
    @TableField("accepted_version_id")
    private Long acceptedVersionId;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 用户决定时间（采纳或拒绝）
     */
    @TableField("decided_at")
    private LocalDateTime decidedAt;
}
