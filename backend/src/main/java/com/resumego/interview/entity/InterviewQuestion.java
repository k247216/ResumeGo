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

    @TableField("generation_ai_invocation_id")
    private Long generationAiInvocationId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}