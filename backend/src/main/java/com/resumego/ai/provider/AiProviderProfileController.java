package com.resumego.ai.provider;

import com.resumego.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/ai")
public class AiProviderProfileController {

    private final AiProviderProfileService service;
    private final String internalToken;

    public AiProviderProfileController(AiProviderProfileService service,
                                       @Value("${resumego.security.internal-token:}") String internalToken) {
        this.service = service;
        this.internalToken = internalToken == null ? "" : internalToken;
    }

    @GetMapping("/providers")
    public ApiResponse<List<AiProviderProfileResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping("/providers")
    public ResponseEntity<ApiResponse<AiProviderProfileResponse>> create(@RequestBody AiProviderProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(request)));
    }

    @PutMapping("/providers/{id}")
    public ApiResponse<AiProviderProfileResponse> update(@PathVariable long id,
                                                         @RequestBody AiProviderProfileRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @PutMapping("/providers/{id}/default")
    public ApiResponse<AiProviderProfileResponse> setDefault(@PathVariable long id) {
        return ApiResponse.ok(service.setDefault(id));
    }

    @DeleteMapping("/providers/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/providers/{id}/test")
    public ApiResponse<AiProviderProfileResponse> test(@PathVariable long id) {
        return ApiResponse.ok(service.test(id));
    }

    @PostMapping("/providers/test")
    public ApiResponse<AiProviderProbeResponse> testUnsaved(@RequestBody AiProviderProbeRequest request) {
        return ApiResponse.ok(service.test(request));
    }

    @PostMapping("/providers/models")
    public ApiResponse<AiProviderProbeResponse> models(@RequestBody AiProviderProbeRequest request) {
        return ApiResponse.ok(service.models(request));
    }

    @PostMapping("/runtime/session")
    public ApiResponse<AiProviderProfileResponse> applyWebSession(@RequestBody AiRuntimeApplyRequest request) {
        return ApiResponse.ok(service.apply(request.profileId(), request.apiKey()));
    }

    @PostMapping("/runtime/apply")
    public ResponseEntity<ApiResponse<AiProviderProfileResponse>> applyDesktop(
            @RequestHeader(value = "X-ResumeGo-Internal", required = false) String providedToken,
            @RequestBody AiRuntimeApplyRequest request
    ) {
        if (!matchesInternalToken(providedToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail("内部运行期凭证无效"));
        }
        return ResponseEntity.ok(ApiResponse.ok(service.apply(request.profileId(), request.apiKey())));
    }

    @DeleteMapping("/runtime/{id}")
    public ApiResponse<AiProviderProfileResponse> clearRuntime(@PathVariable long id) {
        return ApiResponse.ok(service.clearRuntime(id));
    }

    private boolean matchesInternalToken(String provided) {
        if (internalToken.isBlank() || provided == null) return false;
        return MessageDigest.isEqual(internalToken.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
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
