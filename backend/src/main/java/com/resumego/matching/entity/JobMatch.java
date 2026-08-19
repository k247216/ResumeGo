package com.resumego.matching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 岗位匹配结果实体，映射 job_matches 表。
 * 该表是禁飞区输出表——数据由手写算法写入，不由 AI 生成。
 */
@Getter
@Setter
@TableName("job_matches")
public class JobMatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resumeVersionId;

    private Long jobDescriptionId;

    private String algorithmVersion;

    /** 综合匹配度 0-100，check 约束保证范围 */
    private Integer matchScore;

    /** 维度覆盖详情 JSON（MatchDetails 序列化结果） */
    private String detailsJson;

    /** 输入摘要，SHA-256，用于幂等去重 */
    private String inputFingerprint;

    private LocalDateTime createdAt;
}
