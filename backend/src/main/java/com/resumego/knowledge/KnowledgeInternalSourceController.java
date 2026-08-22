package com.resumego.knowledge;

import com.resumego.common.ApiResponse;
import com.resumego.knowledge.dto.KnowledgeManagedSourceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 内部受管原文端点：只接受 main 进程的 X-ResumeGo-Internal 恒定时间校验，
 * renderer 无法直接调用；业务失败返回稳定 code（SOURCE_NOT_FOUND 等）。
 */
@RestController
@RequestMapping("/api/v2/internal/knowledge/documents")
public class KnowledgeInternalSourceController {

    private final KnowledgeInternalSourceService service;
    private final String internalToken;

    public KnowledgeInternalSourceController(
            KnowledgeInternalSourceService service,
            @Value("${resumego.security.internal-token:}") String internalToken) {
        this.service = service;
        this.internalToken = internalToken == null ? "" : internalToken;
    }

    @GetMapping("/{documentId}/managed-source")
    public ResponseEntity<ApiResponse<KnowledgeManagedSourceResponse>> managedSource(
            @RequestHeader(value = "X-ResumeGo-Internal", required = false) String providedToken,
            @PathVariable long documentId) {
        if (!matchesInternalToken(providedToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail("内部运行期凭证无效"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok(service.managedSource(documentId)));
        } catch (ManagedSourceException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail(exception.code()));
        }
    }

    private boolean matchesInternalToken(String provided) {
        if (internalToken.isBlank() || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(internalToken.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }
}
