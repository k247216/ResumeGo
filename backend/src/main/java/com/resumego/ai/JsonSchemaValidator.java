package com.resumego.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 返回 JSON Schema 校验器（雏形）。
 * <p>
 * 对 AI 返回的文本内容进行基本的 JSON Schema 校验：
 * <ul>
 *     <li>是否为合法 JSON</li>
 *     <li>是否包含 Schema 规定的必需字段</li>
 *     <li>字段类型是否匹配</li>
 * </ul>
 * <p>
 * 当前为雏形，后续 Sprint 可扩展为完整的 JSON Schema 规范校验。
 */
public class JsonSchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(JsonSchemaValidator.class);

    private final ObjectMapper objectMapper;

    public JsonSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 校验 AI 返回内容是否符合 Schema 定义。
     *
     * @param aiContent    AI 返回的文本内容
     * @param schemaFields Schema 要求的字段定义
     * @return 校验结果
     */
    public SchemaValidationResult validate(String aiContent, List<SchemaField> schemaFields) {
        List<String> errors = new ArrayList<>();

        if (aiContent == null || aiContent.isBlank()) {
            errors.add("AI 返回内容为空");
            return new SchemaValidationResult(false, errors);
        }

        try {
            JsonNode root = objectMapper.readTree(aiContent);

            for (SchemaField field : schemaFields) {
                JsonNode node = root.get(field.name);
                if (node == null) {
                    if (field.required) {
                        errors.add("缺少必填字段: " + field.name);
                    }
                    continue;
                }
                if (!checkType(node, field.type)) {
                    errors.add(String.format("字段 %s 类型不匹配: 期望 %s, 实际 %s",
                            field.name, field.type, node.getNodeType()));
                }
            }

            boolean valid = errors.isEmpty();
            if (!valid) {
                log.warn("JSON Schema 校验失败: {}", String.join("; ", errors));
            }
            return new SchemaValidationResult(valid, errors);

        } catch (JsonProcessingException e) {
            errors.add("AI 返回不是合法 JSON: " + e.getOriginalMessage());
            log.warn("JSON Schema 校验失败 — 非法 JSON: {}", e.getOriginalMessage());
            return new SchemaValidationResult(false, errors);
        }
    }

    /**
     * 检查 JSON 节点类型是否匹配期望类型。
     */
    private boolean checkType(JsonNode node, SchemaFieldType type) {
        return switch (type) {
            case STRING -> node.isTextual();
            case NUMBER -> node.isNumber();
            case BOOLEAN -> node.isBoolean();
            case ARRAY -> node.isArray();
            case OBJECT -> node.isObject();
        };
    }

    // ── 内部类型 ──

    /**
     * Schema 字段类型。
     */
    public enum SchemaFieldType {
        STRING, NUMBER, BOOLEAN, ARRAY, OBJECT
    }

    /**
     * Schema 单字段定义。
     */
    public record SchemaField(String name, SchemaFieldType type, boolean required) {
        public static SchemaField required(String name, SchemaFieldType type) {
            return new SchemaField(name, type, true);
        }

        public static SchemaField optional(String name, SchemaFieldType type) {
            return new SchemaField(name, type, false);
        }
    }

    /**
     * Schema 校验结果。
     */
    public record SchemaValidationResult(boolean valid, List<String> errors) {
        public boolean isValid() {
            return valid;
        }
    }
}
