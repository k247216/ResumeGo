package com.resumego.interview.controller;

import com.resumego.interview.entity.InterviewerPersona;
import com.resumego.interview.service.InterviewerPersonaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 人设管理协议测试；人设内容仅作为 AI 提示词上下文。 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewerPersonaController 协议映射测试")
class InterviewerPersonaControllerTest {
    @Mock private InterviewerPersonaService personaService;

    @Test
    void shouldListOrMapFailure() {
        when(personaService.listPersonas()).thenReturn(List.of(persona()));
        assertThat(controller().listPersonas().getBody().data()).hasSize(1);
        reset(personaService);
        when(personaService.listPersonas()).thenThrow(new RuntimeException("down"));
        assertThat(controller().listPersonas().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldCreateOrMapFailure() {
        InterviewerPersonaController.CreatePersonaRequest request = request();
        when(personaService.createCustomPersona(anyString(), anyString(), anyString())).thenReturn(persona());
        assertThat(controller().createCustomPersona(request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        reset(personaService);
        when(personaService.createCustomPersona(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("down"));
        assertThat(controller().createCustomPersona(request).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldDeleteOrMapFailures() {
        assertThat(controller().deleteCustomPersona(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(personaService).deleteCustomPersona(1L);
        reset(personaService);
        doThrow(new IllegalArgumentException("不可删除")).when(personaService).deleteCustomPersona(1L);
        assertThat(controller().deleteCustomPersona(1L).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        reset(personaService);
        doThrow(new RuntimeException("down")).when(personaService).deleteCustomPersona(1L);
        assertThat(controller().deleteCustomPersona(1L).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private InterviewerPersonaController controller() { return new InterviewerPersonaController(personaService); }
    private InterviewerPersonaController.CreatePersonaRequest request() { return new InterviewerPersonaController.CreatePersonaRequest("林老师", "架构师", "严谨"); }
    private InterviewerPersona persona() { InterviewerPersona value = new InterviewerPersona(); value.setId(1L); value.setName("林老师"); return value; }
}
