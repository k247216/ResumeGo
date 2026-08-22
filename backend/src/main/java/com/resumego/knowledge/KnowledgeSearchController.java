package com.resumego.knowledge;

import com.resumego.common.ApiResponse;
import com.resumego.knowledge.dto.KnowledgeSearchItemResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v2/knowledge/search")
public class KnowledgeSearchController {

    private final KnowledgeClassificationService service;

    public KnowledgeSearchController(KnowledgeClassificationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<KnowledgeSearchItemResponse>> search(
            @RequestParam("q") String q,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "tagId", required = false) Long tagId) {
        return ApiResponse.ok(service.search(q, categoryId, tagId));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<com.resumego.common.ApiResponse<Void>> notFound(NoSuchElementException exception) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                .body(com.resumego.common.ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<com.resumego.common.ApiResponse<Void>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                .body(com.resumego.common.ApiResponse.fail(exception.getMessage()));
    }
}
