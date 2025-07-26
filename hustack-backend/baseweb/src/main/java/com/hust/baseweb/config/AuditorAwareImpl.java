package com.hust.baseweb.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        var context = SecurityContextHolder.getContext();
        if (context == null) {
            return Optional.of("system");
        }

        var auth = context.getAuthentication();
        if (auth == null) {
            return Optional.of("system");
        }

        if (!auth.isAuthenticated()) {
            throw new IllegalStateException("Authentication is not authenticated");
        }

        if (auth instanceof AnonymousAuthenticationToken) {
            return Optional.of("anonymous");
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof Jwt jwt) {
            String username = jwt.getClaim("preferred_username");

            if (StringUtils.isBlank(username)) {
                throw new IllegalStateException("JWT token does not contain 'preferred_username' claim");
            } else {
                return Optional.of(username);
            }
        }

        throw new IllegalStateException("Unsupported principal type: " + principal.getClass().getName());
    }
}
