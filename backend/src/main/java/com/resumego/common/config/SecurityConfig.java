package com.resumego.common.config;

import com.resumego.common.security.LocalWorkspaceTokenFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public LocalWorkspaceTokenFilter localWorkspaceTokenFilter(
            @Value("${resumego.security.workspace-token:}") String workspaceToken
    ) {
        return new LocalWorkspaceTokenFilter(workspaceToken);
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            LocalWorkspaceTokenFilter localWorkspaceTokenFilter
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(localWorkspaceTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
