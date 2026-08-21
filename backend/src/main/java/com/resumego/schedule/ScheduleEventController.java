package com.resumego.schedule;

import com.resumego.common.ApiResponse;
import com.resumego.schedule.dto.CreateScheduleEventRequest;
import com.resumego.schedule.dto.ScheduleEventResponse;
import com.resumego.schedule.dto.UpdateScheduleEventRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/schedule-events")
public class ScheduleEventController {

    private final ScheduleEventService service;

    public ScheduleEventController(ScheduleEventService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ScheduleEventResponse>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiResponse.ok(service.list(from, to));
    }

    @GetMapping("/{id}")
    public ApiResponse<ScheduleEventResponse> get(@PathVariable long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleEventResponse>> create(
            @Valid @RequestBody CreateScheduleEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(request)));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ScheduleEventResponse> update(@PathVariable long id,
                                                     @Valid @RequestBody UpdateScheduleEventRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable long id) {
        if (!service.delete(id)) {
            throw new NoSuchElementException("日程不存在");
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
