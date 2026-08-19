package com.resumego.job.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobDescriptionDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("jobTitle")
    private String jobTitle;

    @JsonProperty("companyName")
    private String companyName;

    @JsonProperty("rawText")
    private String rawText;

    @JsonProperty("parsed")
    private ParsedJobDescriptionDTO parsed;

    @JsonProperty("parseStatus")
    private String parseStatus;

    @JsonProperty("promptVersion")
    private String promptVersion;

    @JsonProperty("sourceMeta")
    private Object sourceMeta;

    @JsonProperty("jobType")
    private String jobType;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}
