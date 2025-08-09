package com.hust.baseweb.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConfigurationProperties(prefix = "spring.security.session-revocation")
public class SessionRevocationProperties {

    /**
     * Enable/disable session revocation feature
     */
    boolean enabled;

    /**
     * User patterns to check for session revocation
     * Only usernames matching these patterns will be checked
     */
    List<String> usernamePatterns;

    /**
     * Fail-open policy: when service is down, allow requests to pass through
     * If false (default), deny access when service is unavailable (fail-safe)
     */
    boolean failOpen;
}
