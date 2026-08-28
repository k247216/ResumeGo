package com.resumego.interview.entity;


import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

/**
 * 面试问题实体，映射 interview_questions 表。
 * 问题文本由 AI 生成，题号和状态推进由程序控制。
 */
@Getter
@Setter
@TableName("interview_questions")
public class InterviewQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    @TableField("question_index")
    private Integer questionIndex;

    @TableField("question_text")
    private String questionText;

    @TableField("question_type")
    private String questionType;

    @TableField("target_skill")
    private String targetSkill;

    @TableField("source")
    private String source;

    /** 题目来源的稳定引用（题集条目或知识文档片段），随会话保存。 */
    @TableField("source_reference")
    private String sourceReference;

    /** 面向用户的来源说明，例如“真实面经原题”或“资料中未找到依据”。 */
    @TableField("provenance_label")
    private String provenanceLabel;

    @TableField("generation_ai_invocation_id")
    private Long generationAiInvocationId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
