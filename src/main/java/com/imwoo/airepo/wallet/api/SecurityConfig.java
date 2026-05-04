package com.imwoo.airepo.wallet.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AdminHeaderAuthenticationFilter adminHeaderAuthenticationFilter,
            AdminSecurityErrorHandler adminSecurityErrorHandler
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(adminSecurityErrorHandler)
                        .accessDeniedHandler(adminSecurityErrorHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/v1/operational-log-pruning-runs/**")
                        .hasRole(AdminSecurityRole.ADMIN.name())
                        .requestMatchers(
                                "/api/v1/outbox-events/**",
                                "/api/v1/outbox-relay-runs/**",
                                "/api/v1/admin-api-access-audits/**"
                        )
                        .hasRole(AdminSecurityRole.OPERATOR.name())
                        .anyRequest()
                        .permitAll())
                .addFilterBefore(adminHeaderAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
