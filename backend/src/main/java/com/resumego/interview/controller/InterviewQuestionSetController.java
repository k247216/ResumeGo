package com.resumego.interview.controller;

import com.resumego.common.ApiResponse;
import com.resumego.interview.dto.InterviewQuestionSetRequest;
import com.resumego.interview.dto.InterviewQuestionSetResponse;
import com.resumego.interview.dto.InterviewQuestionSetSourcePreviewResponse;
import com.resumego.interview.service.InterviewQuestionSetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 面经题集 API：用户独立本地资产。归档阻止新使用但保留历史。
 */
@RestController
@RequestMapping("/api/v1/interview-question-sets")
public class InterviewQuestionSetController {

    private final InterviewQuestionSetService questionSetService;

    public InterviewQuestionSetController(InterviewQuestionSetService questionSetService) {
        this.questionSetService = questionSetService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InterviewQuestionSetResponse>> create(
            @Valid @RequestBody InterviewQuestionSetRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(questionSetService.create(request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InterviewQuestionSetResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(questionSetService.list()));
    }

    /** 将“真实面经”知识库资料按原题登记为可练习题集。 */
    @PostMapping("/from-knowledge-document/{documentId}")
    public ResponseEntity<ApiResponse<InterviewQuestionSetResponse>> createFromKnowledgeDocument(
            @PathVariable Long documentId) {
        if (documentId == null || documentId <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("知识文档 ID 必须为正数"));
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(questionSetService.createFromKnowledgeDocument(documentId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(e.getMessage()));
        }
    }

    /** 预览知识库面经格式；无副作用，不会创建题集。 */
    @GetMapping("/preview-knowledge-document/{documentId}")
    public ResponseEntity<ApiResponse<InterviewQuestionSetSourcePreviewResponse>> previewKnowledgeDocument(
            @PathVariable Long documentId) {
        if (documentId == null || documentId <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("知识文档 ID 必须为正数"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok(questionSetService.previewKnowledgeDocument(documentId)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(e.getMessage()));
        }
    }

    @GetMapping("/{setId}")
    public ResponseEntity<ApiResponse<InterviewQuestionSetResponse>> get(@PathVariable Long setId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(questionSetService.get(setId)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
        }
    }

    /** 原子替换元数据与题目。 */
    @PatchMapping("/{setId}")
    public ResponseEntity<ApiResponse<InterviewQuestionSetResponse>> update(
            @PathVariable Long setId,
            @Valid @RequestBody InterviewQuestionSetRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(questionSetService.update(setId, request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(e.getMessage()));
        }
    }

    /** 归档：阻止开始新面试，保留历史。 */
    @PostMapping("/{setId}/archive")
    public ResponseEntity<ApiResponse<InterviewQuestionSetResponse>> archive(@PathVariable Long setId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(questionSetService.archive(setId)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
        }
    }
}
