package com.resumego.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class LocalCorsConfigurationTest {

    @Test
    void permitsOnlyLoopbackDesktopAndDevelopmentOrigins() {
        var source = new LocalCorsConfiguration().corsConfigurationSource();
        var request = new MockHttpServletRequest("GET", "/api/v1/resumes");
        var configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOriginPatterns())
                .containsExactly("http://127.0.0.1:*", "http://localhost:*");
        assertThat(configuration.getAllowedHeaders()).contains("X-Workspace-Token", "Content-Type");
        assertThat(configuration.getAllowCredentials()).isFalse();
    }
}
