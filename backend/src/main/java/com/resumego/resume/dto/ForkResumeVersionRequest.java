package com.resumego.resume.dto;

import jakarta.validation.constraints.NotBlank;

/** fork 请求：renderer 只提交新标题；正文由服务端读取源版本复制。 */
public record ForkResumeVersionRequest(
        @NotBlank(message = "简历名称不能为空") String title
) {
}
