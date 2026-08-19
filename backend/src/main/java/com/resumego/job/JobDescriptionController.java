package com.resumego.job;

import com.resumego.common.ApiResponse;
import com.resumego.job.dto.CreateJobDescriptionRequest;
import com.resumego.job.dto.JobDescriptionDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/job-descriptions")
public class JobDescriptionController {

    private final JobDescriptionService jobDescriptionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobDescriptionDTO>>> list() {
        List<JobDescriptionDTO> result = jobDescriptionService.findAllByUser();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{jobDescriptionId}")
    public ResponseEntity<?> getById(@PathVariable Long jobDescriptionId) {
        JobDescriptionDTO result = jobDescriptionService.findById(jobDescriptionId);
        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("JD 不存在或不属于当前用户"));
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JobDescriptionDTO>> create(
            @Valid @RequestBody CreateJobDescriptionRequest request) {
        JobDescriptionDTO result = jobDescriptionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    @PostMapping("/{jobDescriptionId}/parse")
    public ResponseEntity<?> parse(@PathVariable Long jobDescriptionId) {
        JobDescriptionDTO result = jobDescriptionService.parse(jobDescriptionId);
        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("JD 不存在或不属于当前用户"));
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/reparse-all")
    public ResponseEntity<ApiResponse<Integer>> reparseAll() {
        int count = jobDescriptionService.reparseAll();
        return ResponseEntity.ok(ApiResponse.ok(count));
    }

    @DeleteMapping("/{jobDescriptionId}")
    public ResponseEntity<?> delete(@PathVariable Long jobDescriptionId) {
        boolean deleted = jobDescriptionService.delete(jobDescriptionId);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("JD 不存在"));
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/batch-update-job-type")
    public ResponseEntity<ApiResponse<Integer>> batchUpdateJobType(@RequestParam String jobType) {
        int count = jobDescriptionService.batchUpdateJobType(jobType);
        return ResponseEntity.ok(ApiResponse.ok(count));
    }
}
