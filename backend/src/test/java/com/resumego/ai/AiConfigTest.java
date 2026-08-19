package com.resumego.ai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * AiConfig 配置类单元测试
 * 覆盖配置读取、默认值、环境变量覆盖、校验逻辑和辅助方法
 */
@SpringBootTest
class AiConfigTest {

    private AiConfig aiConfig;

    @BeforeEach
    void setUp() {
        aiConfig = new AiConfig();
    }

    @Test
    @DisplayName("默认配置值测试 - endpoint 和 model 应为默认值，apiKey 为空")
    void testDefaultConfiguration() {
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode/v1", aiConfig.getEndpoint());
        assertEquals("qwen-max", aiConfig.getModel());
        assertEquals("", aiConfig.getApiKey());
        assertFalse(aiConfig.isApiKeyConfigured());
    }

    @Test
    @DisplayName("设置配置值测试 - 所有属性应正确设置")
    void testSetConfiguration() {
        String testEndpoint = "https://api.example.com/v1";
        String testModel = "gpt-4";
        String testApiKey = "sk-1234567890";

        aiConfig.setEndpoint(testEndpoint);
        aiConfig.setModel(testModel);
        aiConfig.setApiKey(testApiKey);

        assertEquals(testEndpoint, aiConfig.getEndpoint());
        assertEquals(testModel, aiConfig.getModel());
        assertEquals(testApiKey, aiConfig.getApiKey());
    }

    @Test
    @DisplayName("isApiKeyConfigured 测试 - 当 apiKey 为空时返回 false")
    void testIsApiKeyConfigured_EmptyKey() {
        aiConfig.setApiKey("");
        assertFalse(aiConfig.isApiKeyConfigured());

        aiConfig.setApiKey("   ");
        assertFalse(aiConfig.isApiKeyConfigured());

        aiConfig.setApiKey(null);
        assertFalse(aiConfig.isApiKeyConfigured());
    }

    @Test
    @DisplayName("isApiKeyConfigured 测试 - 当 apiKey 有值时返回 true")
    void testIsApiKeyConfigured_NonEmptyKey() {
        aiConfig.setApiKey("sk-valid-api-key");
        assertTrue(aiConfig.isApiKeyConfigured());

        aiConfig.setApiKey("  sk-with-spaces  ");
        assertTrue(aiConfig.isApiKeyConfigured());
    }

    @Test
    @DisplayName("validate 测试 - 正常配置应通过校验")
    void testValidate_Success() {
        aiConfig.setEndpoint("https://api.example.com");
        aiConfig.setModel("test-model");

        assertDoesNotThrow(() -> aiConfig.validate());
    }

    @Test
    @DisplayName("validate 测试 - 空 endpoint 应抛出异常")
    void testValidate_EmptyEndpoint() {
        aiConfig.setEndpoint("");
        aiConfig.setModel("test-model");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiConfig.validate()
        );
        assertEquals("AI_ENDPOINT 不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("validate 测试 - 空白 endpoint 应抛出异常")
    void testValidate_BlankEndpoint() {
        aiConfig.setEndpoint("   ");
        aiConfig.setModel("test-model");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiConfig.validate()
        );
        assertEquals("AI_ENDPOINT 不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("validate 测试 - 空 model 应抛出异常")
    void testValidate_EmptyModel() {
        aiConfig.setEndpoint("https://api.example.com");
        aiConfig.setModel("");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiConfig.validate()
        );
        assertEquals("AI_MODEL 不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("validate 测试 - 空白 model 应抛出异常")
    void testValidate_BlankModel() {
        aiConfig.setEndpoint("https://api.example.com");
        aiConfig.setModel("   ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiConfig.validate()
        );
        assertEquals("AI_MODEL 不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("toString 测试 - API Key 不应泄露明文")
    void testToString_ApiKeyMasked() {
        aiConfig.setEndpoint("https://api.example.com");
        aiConfig.setModel("test-model");
        aiConfig.setApiKey("sk-secret-12345");

        String result = aiConfig.toString();
        assertTrue(result.contains("endpoint='https://api.example.com'"));
        assertTrue(result.contains("model='test-model'"));
        assertTrue(result.contains("apiKey='***'"));
        assertFalse(result.contains("sk-secret-12345"));
    }

    @Test
    @DisplayName("toString 测试 - 空 API Key 应显示为空字符串")
    void testToString_EmptyApiKey() {
        aiConfig.setEndpoint("https://api.example.com");
        aiConfig.setModel("test-model");
        aiConfig.setApiKey("");

        String result = aiConfig.toString();
        assertTrue(result.contains("apiKey=''"));
    }

    @Test
    @DisplayName("equals 测试 - 相同配置应相等")
    void testEquals_SameConfiguration() {
        AiConfig config1 = new AiConfig();
        config1.setEndpoint("https://api.example.com");
        config1.setModel("test-model");
        config1.setApiKey("sk-123");

        AiConfig config2 = new AiConfig();
        config2.setEndpoint("https://api.example.com");
        config2.setModel("test-model");
        config2.setApiKey("sk-123");

        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    @DisplayName("equals 测试 - 不同配置应不相等")
    void testEquals_DifferentConfiguration() {
        AiConfig config1 = new AiConfig();
        config1.setEndpoint("https://api.example.com");
        config1.setModel("test-model");
        config1.setApiKey("sk-123");

        AiConfig config2 = new AiConfig();
        config2.setEndpoint("https://api.example.com");
        config2.setModel("different-model");
        config2.setApiKey("sk-123");

        assertNotEquals(config1, config2);
    }

    @Test
    @DisplayName("equals 测试 - 与 null 比较应返回 false")
    void testEquals_Null() {
        assertNotEquals(aiConfig, null);
    }

    @Test
    @DisplayName("equals 测试 - 与自身比较应返回 true")
    void testEquals_SameObject() {
        assertEquals(aiConfig, aiConfig);
    }

    @SpringBootTest
    @TestPropertySource(properties = {
            "ai.endpoint=https://custom.endpoint.com",
            "ai.model=custom-model",
            "ai.api-key=sk-custom-env"
    })
    @DisplayName("Spring 集成测试 - 环境变量覆盖配置")
    static class SpringIntegrationTest {

        private final AiConfig aiConfig;

        SpringIntegrationTest(AiConfig aiConfig) {
            this.aiConfig = aiConfig;
        }

        @Test
        void testEnvironmentVariablesOverrideDefaults() {
            assertEquals("https://custom.endpoint.com", aiConfig.getEndpoint());
            assertEquals("custom-model", aiConfig.getModel());
            assertEquals("sk-custom-env", aiConfig.getApiKey());
            assertTrue(aiConfig.isApiKeyConfigured());
        }
    }
}