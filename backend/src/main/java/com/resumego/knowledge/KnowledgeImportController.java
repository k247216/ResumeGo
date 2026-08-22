package com.resumego.knowledge;

import com.resumego.common.ApiResponse;
import com.resumego.knowledge.dto.KnowledgeImportResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v2/knowledge/imports")
public class KnowledgeImportController {

    private final KnowledgeImportService service;

    public KnowledgeImportController(KnowledgeImportService service) {
        this.service = service;
    }

    /** 单文件 multipart；服务端只信任文件字节，不接受任何客户端系统路径。 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<KnowledgeImportResponse>> importFile(
            @RequestParam("file") MultipartFile file) {
        KnowledgeImportResponse response = service.importFile(file);
        HttpStatus status = response.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(ApiResponse.ok(response));
    }

    @ExceptionHandler(KnowledgeImportException.class)
    public ResponseEntity<ApiResponse<Void>> importRejected(KnowledgeImportException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(exception.errorCode() + ": " + exception.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> tooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(KnowledgeErrorCodes.FILE_TOO_LARGE + ": 文件不能超过 10 MiB"));
    }
}
