package com.resumego.knowledge;

import com.resumego.common.ApiResponse;
import com.resumego.knowledge.dto.CreateKnowledgeNameRequest;
import com.resumego.knowledge.dto.KnowledgeCategoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v2/knowledge/categories")
public class KnowledgeCategoryController {

    private final KnowledgeClassificationService service;

    public KnowledgeCategoryController(KnowledgeClassificationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<KnowledgeCategoryResponse>> list() {
        return ApiResponse.ok(service.listCategories());
    }

    /** 重复 normalized 名称返回既有记录（200），新建返回 201。 */
    @PostMapping
    public ResponseEntity<ApiResponse<KnowledgeCategoryResponse>> create(
            @Valid @RequestBody CreateKnowledgeNameRequest request) {
        KnowledgeNameCreateResult<KnowledgeCategoryResponse> result = service.createCategory(request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.ok(result.response()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(exception.getMessage()));
    }
}
