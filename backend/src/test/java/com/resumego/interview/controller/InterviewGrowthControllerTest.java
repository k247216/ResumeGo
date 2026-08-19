package com.resumego.interview.controller;

import com.resumego.interview.dto.InterviewGrowthDimensions;
import com.resumego.interview.dto.InterviewGrowthReportResponse;
import com.resumego.interview.service.InterviewGrowthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("InterviewGrowthController 协议映射测试")
class InterviewGrowthControllerTest {

    @Test
    void shouldReturnGrowthReport() {
        InterviewGrowthService growthService = mock(InterviewGrowthService.class);
        InterviewGrowthReportResponse report = new InterviewGrowthReportResponse(
                10L,
                20L,
                "后端开发实习生",
                "字节跳动",
                List.of(),
                new InterviewGrowthDimensions(0, 0, 0, 0)
        );
        when(growthService.getGrowthReport(10L, 20L)).thenReturn(report);

        var response = new InterviewGrowthController(growthService).getGrowthReport(10L, 20L);

        assertThat(response.getBody().data()).isEqualTo(report);
        verify(growthService).getGrowthReport(10L, 20L);
    }
}
