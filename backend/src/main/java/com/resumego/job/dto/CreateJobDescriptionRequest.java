package com.resumego.job.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateJobDescriptionRequest {

    @NotBlank(message = "岗位名称不能为空")
    @Size(min = 1, max = 200, message = "岗位名称长度需要在 1-200 之间")
    @JsonProperty("jobTitle")
    private String jobTitle;

    @Size(max = 200, message = "公司名称最长 200 字符")
    @JsonProperty("companyName")
    private String companyName;

    @NotBlank(message = "JD 正文不能为空")
    @Size(min = 20, max = 50000, message = "JD 正文长度需要在 20-50000 之间")
    @JsonProperty("rawText")
    private String rawText;

    @JsonProperty("sourceMetaJson")
    private String sourceMetaJson;

    @Size(max = 50, message = "岗位类型最长 50 字符")
    @JsonProperty("jobType")
    private String jobType;
}
