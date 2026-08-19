package com.resumego.interview.service;

import com.resumego.interview.entity.InterviewerPersona;
import com.resumego.interview.mapper.InterviewerPersonaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewerPersonaService 单元测试")
class InterviewerPersonaServiceTest {

    @Mock
    private InterviewerPersonaMapper personaMapper;

    private InterviewerPersonaService personaService;

    @BeforeEach
    void setUp() {
        lenient().when(personaMapper.selectCount(null)).thenReturn(1L);
        personaService = new InterviewerPersonaService(personaMapper);
    }

    @Test
    @DisplayName("创建自定义人设：正常输入")
    void shouldCreateCustomPersona() {
        ArgumentCaptor<InterviewerPersona> captor =
                ArgumentCaptor.forClass(InterviewerPersona.class);

        InterviewerPersona result = personaService.createCustomPersona(
                "张老师", "通用面试官", "温和专业");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("张老师");
        assertThat(result.getTitle()).isEqualTo("通用面试官");
        assertThat(result.getStyle()).isEqualTo("温和专业");
        assertThat(result.getType()).isEqualTo("custom");

        verify(personaMapper).insert(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("张老师");
    }

    @Test
    @DisplayName("姓名超长（>20 字符）应抛出异常")
    void shouldRejectOversizedName() {
        String longName = "a".repeat(21);

        assertThatThrownBy(() -> personaService.createCustomPersona(
                longName, "测试职位", "测试风格"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("姓名过长");
    }

    @Test
    @DisplayName("职位超长（>50 字符）应抛出异常")
    void shouldRejectOversizedTitle() {
        String longTitle = "a".repeat(51);

        assertThatThrownBy(() -> personaService.createCustomPersona(
                "测试", longTitle, "测试风格"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("职位过长");
    }

    @Test
    @DisplayName("风格描述超长（>200 字符）应抛出异常")
    void shouldRejectOversizedStyle() {
        String longStyle = "a".repeat(201);

        assertThatThrownBy(() -> personaService.createCustomPersona(
                "测试", "测试职位", longStyle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("风格描述过长");
    }

    @Test
    @DisplayName("空白姓名字段应抛出异常")
    void shouldRejectBlankName() {
        assertThatThrownBy(() -> personaService.createCustomPersona(
                "   ", "测试职位", "测试风格"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("姓名不能为空");
    }

    @Test
    @DisplayName("空白职位字段应抛出异常")
    void shouldRejectBlankTitle() {
        assertThatThrownBy(() -> personaService.createCustomPersona(
                "测试", "   ", "测试风格"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("职位不能为空");
    }

    @Test
    @DisplayName("空白风格描述应抛出异常")
    void shouldRejectBlankStyle() {
        assertThatThrownBy(() -> personaService.createCustomPersona(
                "测试", "测试职位", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("风格描述不能为空");
    }

    @Test
    @DisplayName("恰好边界值应创建成功")
    void shouldAcceptBoundaryValues() {
        String name20 = "a".repeat(20);
        String title50 = "b".repeat(50);
        String style200 = "c".repeat(200);

        InterviewerPersona result = personaService.createCustomPersona(
                name20, title50, style200);

        assertThat(result).isNotNull();
        verify(personaMapper).insert(any(InterviewerPersona.class));
    }
}