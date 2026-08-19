package com.resumego.company;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CompanyProfileController 接口测试")
class CompanyProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyProfileService companyProfileService;

    @Test
    @DisplayName("根据公司名解析 Profile")
    void shouldResolveCompanyProfileByCompanyName() throws Exception {
        when(companyProfileService.findEnabledProfileByCompanyName("腾讯")).thenReturn(Map.of(
                "companyName", "腾讯",
                "preferenceTags", List.of("工程稳定性", "协作")
        ));

        mockMvc.perform(get("/api/v1/company-profiles/resolve").param("companyName", "腾讯"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("腾讯"))
                .andExpect(jsonPath("$.data.preferenceTags[0]").value("工程稳定性"));
    }

    @Test
    @DisplayName("缺少公司名时返回空 Profile")
    void shouldReturnEmptyProfileWhenCompanyNameMissing() throws Exception {
        when(companyProfileService.findEnabledProfileByCompanyName(isNull())).thenReturn(Map.of());

        mockMvc.perform(get("/api/v1/company-profiles/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isMap());
    }
}
