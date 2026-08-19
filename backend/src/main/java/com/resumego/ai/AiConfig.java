package com.resumego.ai;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * AI 配置类，负责读取和管理 AI 服务相关配置。
 * <p>
 * 配置项通过环境变量注入，支持以下配置：
 * <ul>
 *     <li>AI_ENDPOINT: AI 服务端点 URL</li>
 *     <li>AI_MODEL: AI 模型名称</li>
 *     <li>AI_API_KEY: AI 服务 API 密钥</li>
 * </ul>
 * <p>
 * 默认配置指向阿里云百炼服务，可通过环境变量覆盖。
 */
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    /**
     * AI 服务端点 URL
     * 默认值: https://dashscope.aliyuncs.com/compatible-mode/v1
     */
    private String endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /**
     * AI 模型名称
     * 默认值: qwen-max
     */
    private String model = "qwen-max";

    /**
     * AI 服务 API 密钥
     * 默认值: 空字符串，生产环境必须通过环境变量设置
     */
    private String apiKey = "";

    /**
     * 配置初始化后校验，确保必要的配置项已设置
     */
    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalArgumentException("AI_ENDPOINT 不能为空");
        }
        if (!StringUtils.hasText(model)) {
            throw new IllegalArgumentException("AI_MODEL 不能为空");
        }
    }

    /**
     * 判断 API 密钥是否已配置
     *
     * @return true 如果 API 密钥已配置，false 否则
     */
    public boolean isApiKeyConfigured() {
        return StringUtils.hasText(apiKey);
    }

    /**
     * 获取 AI 服务端点 URL
     *
     * @return AI 服务端点 URL
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * 设置 AI 服务端点 URL
     *
     * @param endpoint AI 服务端点 URL
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * 获取 AI 模型名称
     *
     * @return AI 模型名称
     */
    public String getModel() {
        return model;
    }

    /**
     * 设置 AI 模型名称
     *
     * @param model AI 模型名称
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * 获取 AI 服务 API 密钥
     *
     * @return AI 服务 API 密钥
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * 设置 AI 服务 API 密钥
     *
     * @param apiKey AI 服务 API 密钥
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AiConfig aiConfig = (AiConfig) o;
        return Objects.equals(endpoint, aiConfig.endpoint)
                && Objects.equals(model, aiConfig.model)
                && Objects.equals(apiKey, aiConfig.apiKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, model, apiKey);
    }

    @Override
    public String toString() {
        return "AiConfig{" +
                "endpoint='" + endpoint + '\'' +
                ", model='" + model + '\'' +
                ", apiKey='" + (apiKey != null && !apiKey.isEmpty() ? "***" : "") + '\'' +
                '}';
    }
}