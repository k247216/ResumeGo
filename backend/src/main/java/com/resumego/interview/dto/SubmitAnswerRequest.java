package com.resumego.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 提交回答请求。
 */
public record SubmitAnswerRequest(
        @NotBlank(message = "回答内容不能为空")
        @Size(max = 10_000, message = "回答内容最多 10000 个字符")
        String answerText
) {
}