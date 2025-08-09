package com.hust.baseweb.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SessionRevocationService {

    RedisTemplate<String, Object> redisTemplate;

    static String SESSION_REVOCATION_KEY_PREFIX = "sessions-revocation";

    static String SESSION_REVOCATION_VALUE = "1";

    public boolean isMarked(String realm, String username) {
        if (StringUtils.isBlank(username)) {
            log.debug("Cannot check revocation status: username is blank");
            return false;
        }

        try {
            String redisKey = buildSessionRevocationKey(realm, username);
            boolean isRevoked = redisTemplate.hasKey(redisKey);

            if (isRevoked) {
                log.debug("User is marked as revoked: realm={}, username={}", realm, username);
            }

            return isRevoked;
        } catch (Exception e) {
            log.error("Failed to check revocation status: realm={}, username={}", realm, username, e);
            throw e;
        }
    }

    public void mark(String realm, String username, long ttlSeconds) {
        if (StringUtils.isBlank(username)) {
            log.warn("Cannot mark sessions as revoked: username is blank");
            return;
        }

        String redisKey = buildSessionRevocationKey(realm, username);

        try {
            redisTemplate.opsForValue().set(redisKey, SESSION_REVOCATION_VALUE, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to mark sessions as revoked: realm={}, username={}", realm, username, e);
            throw e;
        }
    }

    public void unmark(String realm, String username) {
        if (StringUtils.isBlank(username)) {
            log.warn("Cannot unmark sessions as revoked: username is blank");
            return;
        }

        try {
            String redisKey = buildSessionRevocationKey(realm, username);
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.error("Failed to unmark sessions as revoked: realm={}, username={}", realm, username, e);
            throw e;
        }
    }

    private String buildSessionRevocationKey(String realm, String username) {
        return String.format("%s:%s:%s", SESSION_REVOCATION_KEY_PREFIX, realm, username);
    }
} 
