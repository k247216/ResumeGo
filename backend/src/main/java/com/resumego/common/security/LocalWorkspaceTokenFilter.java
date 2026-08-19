package com.resumego.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class LocalWorkspaceTokenFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Workspace-Token";

    private final String workspaceToken;

    public LocalWorkspaceTokenFilter(String workspaceToken) {
        this.workspaceToken = workspaceToken == null ? "" : workspaceToken.trim();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !StringUtils.hasText(workspaceToken)
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = request.getHeader(HEADER_NAME);
        if (!workspaceToken.equals(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"UNAUTHORIZED_WORKSPACE\",\"message\":\"本地工作区访问令牌无效\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
