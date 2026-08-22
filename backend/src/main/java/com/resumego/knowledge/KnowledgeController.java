package com.resumego.knowledge;

import com.resumego.common.ApiResponse;
import com.resumego.knowledge.dto.CreateKnowledgeDocumentRequest;
import com.resumego.knowledge.dto.KnowledgeContentResponse;
import com.resumego.knowledge.dto.KnowledgeDocumentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v2/knowledge/documents")
public class KnowledgeController {

    private final KnowledgeService service;
    private final KnowledgeClassificationService classification;

    public KnowledgeController(KnowledgeService service, KnowledgeClassificationService classification) {
        this.service = service;
        this.classification = classification;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<KnowledgeDocumentResponse>> create(
            @Valid @RequestBody CreateKnowledgeDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(request)));
    }

    @GetMapping
    public ApiResponse<List<KnowledgeDocumentResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeDocumentResponse> get(@PathVariable long id) {
        return ApiResponse.ok(service.get(id));
    }

    @GetMapping("/{id}/content")
    public ApiResponse<KnowledgeContentResponse> content(@PathVariable long id) {
        return ApiResponse.ok(service.getContent(id));
    }

    @PutMapping("/{documentId}/category/{categoryId}")
    public ApiResponse<Void> setCategory(@PathVariable long documentId, @PathVariable long categoryId) {
        classification.setDocumentCategory(documentId, categoryId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{documentId}/category/{categoryId}")
    public ApiResponse<Void> removeCategory(@PathVariable long documentId, @PathVariable long categoryId) {
        classification.removeDocumentCategory(documentId, categoryId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{documentId}/tags/{tagId}")
    public ApiResponse<Void> addTag(@PathVariable long documentId, @PathVariable long tagId) {
        classification.addDocumentTag(documentId, tagId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{documentId}/tags/{tagId}")
    public ApiResponse<Void> removeTag(@PathVariable long documentId, @PathVariable long tagId) {
        classification.removeDocumentTag(documentId, tagId);
        return ApiResponse.ok(null);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(exception.getMessage()));
    }
}
