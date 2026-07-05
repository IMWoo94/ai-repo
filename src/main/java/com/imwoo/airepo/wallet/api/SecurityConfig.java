package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.AuthTokenProperties;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private static final String[] OPERATIONAL_API_PATHS = {
            "/api/v1/operational-log-pruning-runs/**",
            "/api/v1/outbox-consumer/**",
            "/api/v1/outbox-events/**",
            "/api/v1/outbox-relay-runs/**",
            "/api/v1/admin-api-access-audits/**",
            "/api/v1/operational-alerts/**",
            "/api/v1/audit-events/**",
            "/api/v1/operations/**",
            "/api/v1/test-fixtures/**"
    };

    @Bean
    @Order(1)
    SecurityFilterChain authSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/v1/auth/**")
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain adminSecurityFilterChain(
            HttpSecurity http,
            AdminHeaderAuthenticationFilter adminHeaderAuthenticationFilter,
            AdminSecurityErrorHandler adminSecurityErrorHandler
    ) throws Exception {
        return http
                .securityMatcher(OPERATIONAL_API_PATHS)
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
                        .requestMatchers(HttpMethod.POST, "/api/v1/outbox-consumer/pruning-runs/**")
                        .hasRole(AdminSecurityRole.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/v1/outbox-events/*/requeue")
                        .hasRole(AdminSecurityRole.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/v1/outbox-events/requeue-requests/*/approve")
                        .hasRole(AdminSecurityRole.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/v1/outbox-events/requeue-requests/*/execute")
                        .hasRole(AdminSecurityRole.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/v1/outbox-events/requeue-requests/*/reject")
                        .hasRole(AdminSecurityRole.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/v1/test-fixtures/**")
                        .hasRole(AdminSecurityRole.ADMIN.name())
                        .anyRequest()
                        .hasRole(AdminSecurityRole.OPERATOR.name()))
                .addFilterBefore(adminHeaderAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain brokerSecurityFilterChain(
            HttpSecurity http,
            BrokerTokenAuthenticationFilter brokerTokenAuthenticationFilter
    ) throws Exception {
        return http
                .securityMatcher("/internal/broker/**")
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .addFilterBefore(brokerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(4)
    SecurityFilterChain walletSecurityFilterChain(
            HttpSecurity http,
            JwtDecoder walletJwtDecoder,
            WalletSecurityErrorHandler walletSecurityErrorHandler
    ) throws Exception {
        return http
                .securityMatcher("/api/v1/wallets/**")
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(walletSecurityErrorHandler))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(walletSecurityErrorHandler)
                        .jwt(jwt -> jwt.decoder(walletJwtDecoder)))
                .build();
    }

    @Bean
    @Order(5)
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    @Bean
    JwtDecoder walletJwtDecoder(AuthTokenProperties properties) {
        SecretKeySpec secretKey = new SecretKeySpec(
                properties.secret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
