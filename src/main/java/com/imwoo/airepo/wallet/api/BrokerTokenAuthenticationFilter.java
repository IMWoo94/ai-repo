package com.imwoo.airepo.wallet.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BrokerTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String BROKER_TOKEN_HEADER = "X-Broker-Token";
    private static final String BROKER_PATH_PREFIX = "/internal/broker/";

    private final BrokerAuthorizationProperties properties;
    private final BrokerSecurityErrorHandler brokerSecurityErrorHandler;

    public BrokerTokenAuthenticationFilter(
            BrokerAuthorizationProperties properties,
            BrokerSecurityErrorHandler brokerSecurityErrorHandler
    ) {
        this.properties = properties;
        this.brokerSecurityErrorHandler = brokerSecurityErrorHandler;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isBrokerPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        String brokerToken = request.getHeader(BROKER_TOKEN_HEADER);
        if (!tokenMatches(properties.brokerToken(), brokerToken)) {
            brokerSecurityErrorHandler.commence(
                    request,
                    response,
                    new BadCredentialsException("broker token is required")
            );
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isBrokerPath(String requestUri) {
        return requestUri != null && requestUri.startsWith(BROKER_PATH_PREFIX);
    }

    private boolean tokenMatches(String expectedToken, String actualToken) {
        if (actualToken == null || actualToken.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                actualToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
