package com.resumego.ai.validate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * AI 输出结构校验兼容器。
 *
 * <p>仅负责 JSON 提取、合法性、必填字段和基础类型校验，不参与简历评分、
 * 岗位匹配排序或面试状态机等禁飞区逻辑。</p>
 */
@Component
public class AiOutputValidator {

    private final ObjectMapper objectMapper;

    public AiOutputValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String extractJson(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                return trimmed.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return trimmed.substring(objectStart, objectEnd + 1);
        }
        return trimmed;
    }

    public ValidationResult validateJson(String json) {
        if (json == null || json.isBlank()) {
            return ValidationResult.invalid(List.of("JSON 内容为空"));
        }
        try {
            objectMapper.readTree(json);
            return ValidationResult.ok();
        } catch (JsonProcessingException exception) {
            return ValidationResult.invalid(List.of("JSON 格式非法"));
        }
    }

    public ValidationResult validateRequiredFields(String json, List<String> requiredFields) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<String> errors = new ArrayList<>();
            for (String field : requiredFields) {
                if (!root.has(field) || root.get(field).isNull()) {
                    errors.add("缺少必填字段: " + field);
                }
            }
            return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.invalid(errors);
        } catch (JsonProcessingException exception) {
            return ValidationResult.invalid(List.of("JSON 格式非法"));
        }
    }

    public ValidationResult validateFieldType(String json, String field, String expectedType) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode node = root.get(field);
            if (node == null || node.isNull()) {
                return ValidationResult.invalid(List.of("字段不存在: " + field));
            }
            boolean matched = switch (expectedType) {
                case "array" -> node.isArray();
                case "object" -> node.isObject();
                case "string" -> node.isTextual();
                case "number" -> node.isNumber();
                case "boolean" -> node.isBoolean();
                default -> false;
            };
            return matched
                    ? ValidationResult.ok()
                    : ValidationResult.invalid(List.of("字段类型不匹配: " + field));
        } catch (JsonProcessingException exception) {
            return ValidationResult.invalid(List.of("JSON 格式非法"));
        }
    }

    /**
     * 校验字段值是否在允许的值域中。
     */
    public ValidationResult validateFieldValue(String json, String field, Set<String> allowedValues) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode node = root.get(field);
            if (node == null || node.isNull()) {
                return ValidationResult.invalid(List.of("字段不存在: " + field));
            }
            if (!node.isTextual()) {
                return ValidationResult.invalid(List.of("字段非字符串类型: " + field));
            }
            String value = node.asText();
            if (!allowedValues.contains(value)) {
                return ValidationResult.invalid(List.of(
                        "字段值非法: " + field + "=" + value + ", 允许值: " + allowedValues));
            }
            return ValidationResult.ok();
        } catch (JsonProcessingException exception) {
            return ValidationResult.invalid(List.of("JSON 格式非法"));
        }
    }

    public record ValidationResult(boolean valid, List<String> errors) {
        public static ValidationResult ok() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult invalid(List<String> errors) {
            return new ValidationResult(false, errors);
        }

        public boolean isValid() {
            return valid;
        }
    }
}
