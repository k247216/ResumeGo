package com.resumego.job.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ParsedJobDescriptionDTO {

    @JsonProperty("requiredSkills")
    private List<String> requiredSkills;

    @JsonProperty("preferredSkills")
    private List<String> preferredSkills;

    @JsonProperty("responsibilities")
    private List<String> responsibilities;

    @JsonProperty("experienceRequirements")
    private List<String> experienceRequirements;

    @JsonProperty("educationRequirements")
    private List<String> educationRequirements;
}
