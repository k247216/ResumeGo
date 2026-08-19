package com.resumego.job;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("job_descriptions")
public class JobDescription {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String jobTitle;

    private String companyName;

    private String rawText;

    private String parsedJson;

    private String parseStatus;

    private String promptVersion;

    private Long parseAiInvocationId;

    private String sourceMetaJson;

    private String jobType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
