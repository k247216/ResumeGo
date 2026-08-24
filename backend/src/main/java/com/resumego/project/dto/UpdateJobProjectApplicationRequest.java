package com.resumego.project.dto;

import jakarta.validation.constraints.Size;

public record UpdateJobProjectApplicationRequest(
        @Size(max = 60, message = "行业不能超过 60 个字符")
        String industry,
        @Size(max = 120, message = "期望岗位不能超过 120 个字符")
        String role,
        @Size(max = 120, message = "地点不能超过 120 个字符")
        String location,
        @Size(max = 500, message = "备注不能超过 500 个字符")
        String notes
) {
}
