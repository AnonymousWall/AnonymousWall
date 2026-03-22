package com.anonymous.wall.security;

import io.lettuce.core.api.StatefulRedisConnection;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Redis-backed rate limiting service using fixed-window counters.
 *
 * <p>Each unique key gets a Redis counter that increments on every call
 * and expires after the configured window.  Multi-instance safe because
 * all nodes share the same Redis.</p>
 *
 * <p>Fails open: if Redis is unreachable the request is allowed so that
 * a Redis outage doesn't lock out every user.</p>
 */
@Singleton
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);
    private static final String KEY_PREFIX = "rl:";

    @Inject
    private StatefulRedisConnection<String, String> redisConnection;

    @Value("${rate-limit.enabled:true}")
    private boolean enabled;

    /**
     * Check whether the caller should be rate-limited.
     *
     * @param key         logical bucket name, e.g. {@code "send-code:email:alice@school.edu"}
     * @param maxAttempts maximum allowed requests inside {@code window}
     * @param window      time window for the counter
     * @return result indicating whether the request is blocked
     */
    public RateLimitResult checkRateLimit(String key, int maxAttempts, Duration window) {
        if (!enabled) {
            return RateLimitResult.allowed(maxAttempts);
        }

        String redisKey = KEY_PREFIX + key;
        try {
            var commands = redisConnection.sync();

            // Atomic increment
            Long count = commands.incr(redisKey);
            if (count == null) {
                return RateLimitResult.allowed(maxAttempts);
            }

            // Set expiry on first request in the window
            if (count == 1L) {
                commands.expire(redisKey, window.getSeconds());
            } else {
                // Safety net: if key lost its TTL (crash between INCR and EXPIRE), reapply
                Long keyTtl = commands.ttl(redisKey);
                if (keyTtl != null && keyTtl == -1L) {
                    commands.expire(redisKey, window.getSeconds());
                }
            }

            long ttl = Math.max(commands.ttl(redisKey), 0);
            int remaining = (int) Math.max(maxAttempts - count, 0);

            if (count > maxAttempts) {
                log.warn("Rate limit exceeded: key={}, count={}, max={}", redisKey, count, maxAttempts);
                return new RateLimitResult(true, 0, ttl);
            }

            return new RateLimitResult(false, remaining, ttl);
        } catch (Exception e) {
            // Fail open — never block legitimate users because Redis is down
            log.error("Rate limit check failed (allowing request): key={}, error={}", redisKey, e.getMessage());
            return RateLimitResult.allowed(maxAttempts);
        }
    }

    // ------------------------------------------------------------------ //

    /**
     * Immutable result of a rate-limit check.
     */
    public static class RateLimitResult {
        private final boolean limited;
        private final int remaining;
        private final long retryAfterSeconds;

        public RateLimitResult(boolean limited, int remaining, long retryAfterSeconds) {
            this.limited = limited;
            this.remaining = remaining;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        /** Convenience factory for the "allowed" case. */
        public static RateLimitResult allowed(int remaining) {
            return new RateLimitResult(false, remaining, 0);
        }

        public boolean isLimited()          { return limited; }
        public int     getRemaining()       { return remaining; }
        public long    getRetryAfterSeconds() { return retryAfterSeconds; }
    }
}
