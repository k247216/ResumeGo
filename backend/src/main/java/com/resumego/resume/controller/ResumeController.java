package com.resumego.resume.controller;

import com.resumego.common.ApiResponse;
import com.resumego.resume.dto.CreateResumeRequest;
import com.resumego.resume.dto.CreateResumeVersionRequest;
import com.resumego.resume.dto.ForkResumeVersionRequest;
import com.resumego.resume.dto.ResumeDTO;
import com.resumego.resume.dto.ResumeVersionDTO;
import com.resumego.resume.dto.UpdateResumeAssetRequest;
import com.resumego.resume.dto.UpdateResumeTargetJobRequest;
import com.resumego.resume.dto.UpdateResumeVersionSummaryRequest;
import com.resumego.resume.service.ResumeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @DeleteMapping("/resumes/{resumeId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long resumeId) {
        if (!resumeService.deleteResume(resumeId)) {
            throw new NoSuchElementException("简历不存在");
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/resumes")
    public ResponseEntity<ApiResponse<List<ResumeDTO>>> list(
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) Boolean archived) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(resumeService.listAssets(kind, archived)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PostMapping("/resumes")
    public ResponseEntity<ApiResponse<ResumeDTO>> create(
            @Valid @RequestBody CreateResumeRequest request) {
        try {
            ResumeDTO resume = resumeService.createResume(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(resume));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PatchMapping("/resumes/{resumeId}/target-job")
    public ResponseEntity<ApiResponse<ResumeDTO>> updateTargetJob(
            @PathVariable Long resumeId,
            @RequestBody UpdateResumeTargetJobRequest request) {
        try {
            ResumeDTO resume = resumeService.updateTargetJob(resumeId, request);
            return ResponseEntity.ok(ApiResponse.ok(resume));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @GetMapping("/resume-versions/{versionId}")
    public ResponseEntity<?> getVersion(@PathVariable Long versionId) {
        try {
            ResumeVersionDTO version = resumeService.getVersion(versionId);
            return ResponseEntity.ok(ApiResponse.ok(version));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail(e.getMessage()));
        }
    }

    @GetMapping("/resumes/{resumeId}/versions")
    public ResponseEntity<ApiResponse<List<ResumeVersionDTO>>> getVersions(
            @PathVariable Long resumeId) {
        try {
            List<ResumeVersionDTO> versions = resumeService.getVersions(resumeId);
            return ResponseEntity.ok(ApiResponse.ok(versions));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PostMapping("/resumes/{resumeId}/versions")
    public ResponseEntity<ApiResponse<ResumeVersionDTO>> createManualVersion(
            @PathVariable Long resumeId,
            @Valid @RequestBody CreateResumeVersionRequest request) {
        try {
            ResumeVersionDTO version = resumeService.createManualVersion(resumeId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(version));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PatchMapping("/resume-versions/{versionId}/summary")
    public ResponseEntity<ApiResponse<ResumeVersionDTO>> updateVersionSummary(
            @PathVariable Long versionId,
            @Valid @RequestBody UpdateResumeVersionSummaryRequest request) {
        try {
            ResumeVersionDTO version = resumeService.updateVersionSummary(
                    versionId, request == null ? null : request.changeSummary());
            return ResponseEntity.ok(ApiResponse.ok(version));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
        }
    }

    /** 从任意版本创建岗位表达副本：只接受新标题，正文由服务端复制。 */
    @PostMapping("/resume-versions/{versionId}/fork")
    public ResponseEntity<ApiResponse<ResumeDTO>> forkVersion(
            @PathVariable Long versionId,
            @Valid @RequestBody ForkResumeVersionRequest request) {
        try {
            ResumeDTO forked = resumeService.forkVersion(versionId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(forked));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
        }
    }

    /** 简历资产改名。 */
    @PatchMapping("/resumes/{resumeId}")
    public ResponseEntity<ApiResponse<ResumeDTO>> rename(
            @PathVariable Long resumeId,
            @Valid @RequestBody UpdateResumeAssetRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(resumeService.renameResume(resumeId, request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
        }
    }

    /** 归档简历资产（不删除历史与关联）。 */
    @PostMapping("/resumes/{resumeId}/archive")
    public ResponseEntity<ApiResponse<ResumeDTO>> archive(@PathVariable Long resumeId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(resumeService.archiveResume(resumeId)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
        }
    }

    /** 恢复归档的简历资产。 */
    @PostMapping("/resumes/{resumeId}/restore")
    public ResponseEntity<ApiResponse<ResumeDTO>> restore(@PathVariable Long resumeId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(resumeService.restoreResume(resumeId)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
        }
    }
}
