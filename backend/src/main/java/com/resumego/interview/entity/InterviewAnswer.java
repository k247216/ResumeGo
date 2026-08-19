package com.resumego.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 面试回答实体，映射 interview_answers 表。
 * 回答原文不写入普通应用日志。
 */
@Getter
@Setter
@TableName("interview_answers")
public class InterviewAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    @TableField("question_id")
    private Long questionId;

    @TableField("answer_text")
    private String answerText;

    @TableField("created_at")
    private LocalDateTime createdAt;
}