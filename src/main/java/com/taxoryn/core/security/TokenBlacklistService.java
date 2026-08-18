package com.taxoryn.core.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to maintain invalidated / logged-out JWT tokens.
 * Automatically evicts expired tokens to keep memory footprint minimal.
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private final Map<String, Date> blacklistedTokens = new ConcurrentHashMap<>();

    /**
     * Blacklist a token until its natural expiration date.
     *
     * @param token JWT token string or JTI
     * @param expiryDate Expiration timestamp of the token
     */
    public void blacklistToken(String token, Date expiryDate) {
        if (token != null && expiryDate != null) {
            blacklistedTokens.put(token, expiryDate);
            log.debug("Token blacklisted until {}", expiryDate);
        }
    }

    /**
     * Check if token is blacklisted.
     *
     * @param token JWT token string
     * @return true if blacklisted, false otherwise
     */
    public boolean isBlacklisted(String token) {
        if (token == null) {
            return false;
        }
        Date expiryDate = blacklistedTokens.get(token);
        if (expiryDate == null) {
            return false;
        }
        if (expiryDate.before(new Date())) {
            blacklistedTokens.remove(token);
            return false;
        }
        return true;
    }

    /**
     * Clean up expired tokens every 30 minutes.
     */
    @Scheduled(fixedRate = 1800000)
    public void cleanupExpiredTokens() {
        Date now = new Date();
        int initialSize = blacklistedTokens.size();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().before(now));
        log.debug("Evicted {} expired tokens from blacklist cache", initialSize - blacklistedTokens.size());
    }
}
