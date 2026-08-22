package com.resumego.knowledge;

import com.resumego.common.ApiResponse;
import com.resumego.knowledge.dto.CreateKnowledgeCategoryRequest;
import com.resumego.knowledge.dto.KnowledgeCategoryNodeResponse;
import com.resumego.knowledge.dto.KnowledgeCategoryResponse;
import com.resumego.knowledge.dto.UpdateKnowledgeCategoryRequest;
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
    public ApiResponse<List<KnowledgeCategoryNodeResponse>> list() {
        return ApiResponse.ok(service.listCategoryTree());
    }

    /** 重复 normalized 名称返回既有记录（200），新建返回 201；parentId 可为 null（根节点）。 */
    @PostMapping
    public ResponseEntity<ApiResponse<KnowledgeCategoryResponse>> create(
            @Valid @RequestBody CreateKnowledgeCategoryRequest request) {
        KnowledgeNameCreateResult<KnowledgeCategoryResponse> result = service.createCategory(request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.ok(result.response()));
    }

    /** 完整更新 { name, parentId }，两字段必须显式出现；parentId 可为 null（移到根）。 */
    @PutMapping("/{id}")
    public ApiResponse<KnowledgeCategoryResponse> update(
            @PathVariable long id,
            @Valid @RequestBody UpdateKnowledgeCategoryRequest request) {
        return ApiResponse.ok(service.updateCategory(id, request));
    }

    /** 仅删除空叶节点（无子分类且无直属文档），否则 409 CATEGORY_NOT_EMPTY。 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        service.deleteCategory(id);
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(exception.getMessage()));
    }
}
