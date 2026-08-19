package com.resumego.layout;

import com.resumego.common.ApiResponse;
import com.resumego.layout.dto.LayoutProposalRequest;
import com.resumego.layout.dto.LayoutProposalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 排版助手接口。
 */
@RestController
public class LayoutProposalController {

    private static final Logger log = LoggerFactory.getLogger(LayoutProposalController.class);

    private final LayoutProposalService layoutProposalService;

    public LayoutProposalController(LayoutProposalService layoutProposalService) {
        this.layoutProposalService = layoutProposalService;
    }

    @PostMapping("/api/v1/resume-layout/proposals")
    public ResponseEntity<ApiResponse<LayoutProposalResponse>> generateProposal(
            @RequestBody LayoutProposalRequest request) {
        try {
            LayoutProposalResponse response = layoutProposalService.generateProposal(request);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (IllegalArgumentException e) {
            log.warn("生成排版提案参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("生成排版提案失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.fail("生成排版提案失败"));
        }
    }
}
