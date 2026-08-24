package com.resumego.knowledge;

import com.resumego.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v2/knowledge/cleanup-jobs")
public class KnowledgeCleanupJobController {

    private final KnowledgeRecoveryService recovery;

    public KnowledgeCleanupJobController(KnowledgeRecoveryService recovery) {
        this.recovery = recovery;
    }

    /** 只允许当前用户 FAILED/PENDING job；启动恢复只处理 PENDING，避免无限重试 FAILED。 */
    @PostMapping("/{id}/retry")
    public ApiResponse<Void> retry(@PathVariable long id) {
        recovery.retryCleanupJob(id);
        return ApiResponse.ok(null);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(NoSuchElementException exception) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(IllegalStateException exception) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                .body(ApiResponse.fail(exception.getMessage()));
    }
}
