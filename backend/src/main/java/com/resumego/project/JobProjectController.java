package com.resumego.project;

import com.resumego.common.ApiResponse;
import com.resumego.project.dto.CreateJobProjectRequest;
import com.resumego.project.dto.JobProjectResponse;
import com.resumego.project.dto.RenameJobProjectRequest;
import com.resumego.project.dto.StageEventResponse;
import com.resumego.project.dto.UpdateJobProjectApplicationRequest;
import com.resumego.project.dto.UpdateJobProjectLinksRequest;
import com.resumego.project.dto.UpdateJobProjectStageRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/projects")
public class JobProjectController {

    private final JobProjectService service;

    public JobProjectController(JobProjectService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<JobProjectResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<JobProjectResponse> get(@PathVariable long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JobProjectResponse>> create(@Valid @RequestBody CreateJobProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(request)));
    }

    @PatchMapping("/{id}/name")
    public ApiResponse<JobProjectResponse> rename(@PathVariable long id,
                                                   @Valid @RequestBody RenameJobProjectRequest request) {
        return ApiResponse.ok(service.rename(id, request));
    }

    @PatchMapping("/{id}/links")
    public ApiResponse<JobProjectResponse> updateLinks(@PathVariable long id,
                                                        @RequestBody UpdateJobProjectLinksRequest request) {
        return ApiResponse.ok(service.updateLinks(id, request));
    }

    @PatchMapping("/{id}/stage")
    public ApiResponse<JobProjectResponse> updateStage(@PathVariable long id,
                                                       @Valid @RequestBody UpdateJobProjectStageRequest request) {
        return ApiResponse.ok(service.updateStage(id, request));
    }

    @GetMapping("/{id}/stage-events")
    public ApiResponse<List<StageEventResponse>> stageEvents(@PathVariable long id) {
        return ApiResponse.ok(service.listStageEvents(id));
    }

    @PatchMapping("/{id}/application")
    public ApiResponse<JobProjectResponse> updateApplicationInfo(@PathVariable long id,
                                                                 @Valid @RequestBody UpdateJobProjectApplicationRequest request) {
        return ApiResponse.ok(service.updateApplicationInfo(id, request));
    }

    @PostMapping("/{id}/archive")
    public ApiResponse<JobProjectResponse> archive(@PathVariable long id) {
        return ApiResponse.ok(service.archive(id));
    }

    @PostMapping("/{id}/restore")
    public ApiResponse<JobProjectResponse> restore(@PathVariable long id) {
        return ApiResponse.ok(service.restore(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable long id) {
        if (!service.delete(id)) {
            throw new NoSuchElementException("求职项目不存在");
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(exception.getMessage()));
    }
}
