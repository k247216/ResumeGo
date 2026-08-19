package com.resumego.optimization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AI 建议追问后的补充事实请求。
 *
 * @param userSupplement 用户补充的事实信息，只用于生成最终建议，不直接写入简历
 */
public record SuggestionFollowUpRequest(
        @NotBlank(message = "补充事实不能为空")
        @Size(min = 8, max = 1200, message = "补充事实长度需在 8-1200 字之间")
        String userSupplement
) {
}
