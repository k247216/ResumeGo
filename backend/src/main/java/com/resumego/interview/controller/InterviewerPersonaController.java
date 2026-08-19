package com.resumego.interview.controller;

import com.resumego.common.ApiResponse;
import com.resumego.interview.entity.InterviewerPersona;
import com.resumego.interview.service.InterviewerPersonaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequiredArgsConstructor
public class InterviewerPersonaController {

    private static final Logger log = LoggerFactory.getLogger(InterviewerPersonaController.class);

    private final InterviewerPersonaService personaService;

    /**
     * 获取所有可用面试官人设。
     * GET /api/v1/interviewer-personas
     */
    @GetMapping("/api/v1/interviewer-personas")
    public ResponseEntity<ApiResponse<List<InterviewerPersona>>> listPersonas() {
        try {
            List<InterviewerPersona> personas = personaService.listPersonas();
            return ResponseEntity.ok(ApiResponse.ok(personas));
        } catch (Exception e) {
            log.error("获取面试官人设列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("服务内部错误"));
        }
    }

    /**
     * 创建自定义面试官人设。
     * POST /api/v1/interviewer-personas
     */
    @PostMapping("/api/v1/interviewer-personas")
    public ResponseEntity<ApiResponse<InterviewerPersona>> createCustomPersona(
            @Valid @RequestBody CreatePersonaRequest request) {
        try {
            InterviewerPersona persona = personaService.createCustomPersona(
                    request.name(), request.title(), request.style());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(persona));
        } catch (Exception e) {
            log.error("创建自定义人设失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("服务内部错误"));
        }
    }

    /**
     * 删除自定义面试官人设。
     * DELETE /api/v1/interviewer-personas/{id}
     */
    @DeleteMapping("/api/v1/interviewer-personas/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomPersona(
            @PathVariable @Positive(message = "id 必须为正整数") Long id) {
        try {
            personaService.deleteCustomPersona(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IllegalArgumentException e) {
            log.warn("删除人设失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("删除人设时发生未预期错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("服务内部错误"));
        }
    }

    public record CreatePersonaRequest(
            @NotBlank(message = "姓名不能为空")
            @Size(max = 20, message = "姓名最多20个字符")
            String name,
            @NotBlank(message = "职位不能为空")
            @Size(max = 50, message = "职位最多50个字符")
            String title,
            @NotBlank(message = "风格描述不能为空")
            @Size(max = 200, message = "风格描述最多200个字符")
            String style
    ) {}
}