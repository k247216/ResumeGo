package com.resumego.interview.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 跨会话总结请求。
 */
public record MultiSessionSummaryRequest(
        @NotEmpty(message = "至少选择一个会话")
        @Size(min = 1, max = 10, message = "最多选择 10 个会话")
        List<Long> sessionIds
) {
}