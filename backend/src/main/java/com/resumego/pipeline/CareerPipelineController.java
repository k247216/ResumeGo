package com.resumego.pipeline;

import com.resumego.common.ApiResponse;
import com.resumego.pipeline.dto.CareerPipelineResponse;
import com.resumego.pipeline.dto.CreateCareerPipelineRequest;
import com.resumego.pipeline.dto.AddPipelineStageRequest;
import com.resumego.pipeline.dto.RenamePipelineStageRequest;
import com.resumego.pipeline.dto.ReorderPipelineStagesRequest;
import com.resumego.pipeline.dto.PipelineStageTransitionResponse;
import com.resumego.pipeline.dto.TransitionPipelineStageRequest;
import com.resumego.pipeline.dto.UpdateCareerPipelineRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v2/pipelines")
public class CareerPipelineController {

    private final CareerPipelineService service;

    public CareerPipelineController(CareerPipelineService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<CareerPipelineResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<CareerPipelineResponse> get(@PathVariable long id) {
        return ApiResponse.ok(service.get(id));
    }

    @GetMapping("/{id}/transitions")
    public ApiResponse<List<PipelineStageTransitionResponse>> transitions(@PathVariable long id) {
        return ApiResponse.ok(service.findTransitionHistory(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CareerPipelineResponse>> create(
            @Valid @RequestBody CreateCareerPipelineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(request)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CareerPipelineResponse>> update(
            @PathVariable long id,
            @Valid @RequestBody UpdateCareerPipelineRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, request)));
    }

    @PostMapping("/{id}/transitions")
    public ApiResponse<CareerPipelineResponse> transition(
            @PathVariable long id,
            @Valid @RequestBody TransitionPipelineStageRequest request) {
        return ApiResponse.ok(service.transition(id, request));
    }

    @PostMapping("/{id}/stages")
    public ResponseEntity<ApiResponse<CareerPipelineResponse>> addStage(
            @PathVariable long id,
            @Valid @RequestBody AddPipelineStageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.addStage(id, request)));
    }

    @PatchMapping("/{id}/stages/{stageId}")
    public ApiResponse<CareerPipelineResponse> renameStage(
            @PathVariable long id,
            @PathVariable long stageId,
            @Valid @RequestBody RenamePipelineStageRequest request) {
        return ApiResponse.ok(service.renameStage(id, stageId, request));
    }

    @PutMapping("/{id}/stages/order")
    public ApiResponse<CareerPipelineResponse> reorderStages(
            @PathVariable long id,
            @Valid @RequestBody ReorderPipelineStagesRequest request) {
        return ApiResponse.ok(service.reorderStages(id, request));
    }

    @PutMapping("/{id}/schedule-events/{eventId}")
    public ApiResponse<CareerPipelineResponse> linkScheduleEvent(
            @PathVariable long id, @PathVariable long eventId) {
        return ApiResponse.ok(service.linkScheduleEvent(id, eventId));
    }

    @DeleteMapping("/{id}/schedule-events/{eventId}")
    public ApiResponse<CareerPipelineResponse> unlinkScheduleEvent(
            @PathVariable long id, @PathVariable long eventId) {
        return ApiResponse.ok(service.unlinkScheduleEvent(id, eventId));
    }

    @PutMapping("/{id}/interview-plans/{planId}")
    public ApiResponse<CareerPipelineResponse> linkInterviewPlan(
            @PathVariable long id, @PathVariable long planId) {
        return ApiResponse.ok(service.linkInterviewPlan(id, planId));
    }

    @DeleteMapping("/{id}/interview-plans/{planId}")
    public ApiResponse<CareerPipelineResponse> unlinkInterviewPlan(
            @PathVariable long id, @PathVariable long planId) {
        return ApiResponse.ok(service.unlinkInterviewPlan(id, planId));
    }

    @PostMapping("/{id}/archive")
    public ApiResponse<CareerPipelineResponse> archive(@PathVariable long id) {
        return ApiResponse.ok(service.archive(id));
    }

    @PostMapping("/{id}/restore")
    public ApiResponse<CareerPipelineResponse> restore(@PathVariable long id) {
        return ApiResponse.ok(service.restore(id));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> badRequest(RuntimeException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(exception.getMessage()));
    }
}
