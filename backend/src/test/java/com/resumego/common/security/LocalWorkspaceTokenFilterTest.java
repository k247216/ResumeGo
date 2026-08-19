package com.resumego.common.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class LocalWorkspaceTokenFilterTest {

    @Test
    @DisplayName("/api 请求缺少本地工作区令牌时返回 401")
    void rejectApiRequestWithoutWorkspaceToken() throws Exception {
        LocalWorkspaceTokenFilter filter = new LocalWorkspaceTokenFilter("local-token");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/interviews/my");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("/api 请求携带正确本地工作区令牌时放行")
    void allowApiRequestWithWorkspaceToken() throws Exception {
        LocalWorkspaceTokenFilter filter = new LocalWorkspaceTokenFilter("local-token");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/interviews/my");
        request.addHeader("X-Workspace-Token", "local-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
    }
}
