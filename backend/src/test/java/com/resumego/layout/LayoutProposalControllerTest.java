package com.resumego.layout;

import com.resumego.common.ApiResponse;
import com.resumego.layout.dto.LayoutProposalRequest;
import com.resumego.layout.dto.LayoutProposalResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** 排版提案接口的响应边界回归测试，不涉及禁飞区。 */
@ExtendWith(MockitoExtension.class)
class LayoutProposalControllerTest {

    @Mock
    private LayoutProposalService layoutProposalService;

    @InjectMocks
    private LayoutProposalController controller;

    private final LayoutProposalRequest request = new LayoutProposalRequest(
            1L, Map.of("summary", "示例摘要"), null, Map.of(), "classic", "compress_to_one_page"
    );

    @Test
    void shouldReturnProposalPayloadOnSuccess() {
        LayoutProposalResponse proposal = new LayoutProposalResponse(
                "proposal-1", "mock", "layout-v1", List.of(), "classic", List.of(), List.of()
        );
        when(layoutProposalService.generateProposal(request)).thenReturn(proposal);

        ResponseEntity<ApiResponse<LayoutProposalResponse>> response = controller.generateProposal(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data()).isSameAs(proposal);
    }

    @Test
    void shouldReturnBadRequestForInvalidRequest() {
        when(layoutProposalService.generateProposal(request)).thenThrow(new IllegalArgumentException("草稿为空"));

        ResponseEntity<ApiResponse<LayoutProposalResponse>> response = controller.generateProposal(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("草稿为空");
    }

    @Test
    void shouldHideInternalExceptionDetail() {
        when(layoutProposalService.generateProposal(request)).thenThrow(new RuntimeException("provider credential detail"));

        ResponseEntity<ApiResponse<LayoutProposalResponse>> response = controller.generateProposal(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("生成排版提案失败");
    }
}
