package com.resumego.knowledge;

import com.resumego.common.ApiResponse;
import com.resumego.knowledge.dto.CreateKnowledgeNameRequest;
import com.resumego.knowledge.dto.KnowledgeTagResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v2/knowledge/tags")
public class KnowledgeTagController {

    private final KnowledgeClassificationService service;

    public KnowledgeTagController(KnowledgeClassificationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<KnowledgeTagResponse>> list() {
        return ApiResponse.ok(service.listTags());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<KnowledgeTagResponse>> create(
            @Valid @RequestBody CreateKnowledgeNameRequest request) {
        KnowledgeNameCreateResult<KnowledgeTagResponse> result = service.createTag(request);
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
