package com.hust.baseweb.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.baseweb.config.KeycloakAdminProperties;
import com.hust.baseweb.config.SessionRevocationProperties;
import com.hust.baseweb.dto.response.ApiResponse;
import com.hust.baseweb.dto.response.ErrorCode;
import com.hust.baseweb.service.SessionRevocationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to check session revocation in each request. This filter runs after JWT authentication is successful
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SessionRevocationFilter extends OncePerRequestFilter {

    SessionRevocationService sessionRevocationService;

    KeycloakAdminProperties keycloakAdminProperties;

    SessionRevocationProperties sessionRevocationProperties;

    ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwtAuth &&
                jwtAuth.isAuthenticated() &&
                jwtAuth.getPrincipal() instanceof Jwt jwt) {

                processJwtAuthentication(jwt, request, response, filterChain);
            } else {
                filterChain.doFilter(request, response);
            }
        } catch (Exception e) {
            handleFilterError(e, request, response, filterChain);
        }
    }

    private void processJwtAuthentication(
        Jwt jwt,
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String username = extractUsername(jwt);

        if (shouldCheckUser(username)) {
            if (isUserSessionsRevoked(jwt)) {
                handleRevokedUser(username, response);
            } else {
                filterChain.doFilter(request, response);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean isUserSessionsRevoked(Jwt jwt) {
        String username = extractUsername(jwt);

        if (StringUtils.isBlank(username)) {
            log.debug("Cannot check revocation status: username is blank");
            return false;
        }

        try {
            return sessionRevocationService.isMarked(keycloakAdminProperties.getRealm(), username);
        } catch (Exception e) {
            log.error("Error checking session revocation: username={}", username, e);

            // Apply the fail-safe/fail-open policy
            if (sessionRevocationProperties.isFailOpen()) {
                log.debug("Fail-open policy: allowing access when service is down");
                return false; // Allow access
            } else {
                log.debug("Fail-safe policy: denying access when service is down");
                return true; // Deny access (secure by default)
            }
        }
    }

    private boolean shouldCheckUser(String username) {
        if (StringUtils.isBlank(username)) {
            return false;
        }

        return sessionRevocationProperties.getUsernamePatterns().stream().anyMatch(username::startsWith);
    }

    private void handleRevokedUser(String username, HttpServletResponse response) {
        log.debug("Request rejected due to revoked user sessions: username={}", username);

        // Clear SecurityContext
        SecurityContextHolder.clearContext();

        // Send unauthorized response
        sendUnauthorizedResponse(response);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> apiResponse = ApiResponse.of(ErrorCode.SESSION_REVOKED);

        try {
            String jsonResponse = objectMapper.writeValueAsString(apiResponse);
            response.getWriter().write(jsonResponse);
        } catch (IOException e) {
            log.error("Failed to write error response", e);
        }
    }

    private void handleFilterError(
        Exception e,
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        log.error("Error in session revocation filter", e);

        if (sessionRevocationProperties.isFailOpen()) {
            log.info("Fail-open policy: allowing request to pass through despite error");
            filterChain.doFilter(request, response);
        } else {
            log.warn("Fail-safe policy: denying access due to filter error");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private String extractUsername(Jwt jwt) {
        try {
            return jwt.getClaimAsString("preferred_username");
        } catch (Exception e) {
            log.error("Could not extract claim preferred_username from JWT", e);
            return null;
        }
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !sessionRevocationProperties.isEnabled();
    }
} 
