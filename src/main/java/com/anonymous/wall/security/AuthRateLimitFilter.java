package com.anonymous.wall.security;

import io.micronaut.context.annotation.Value;
import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.filter.ServerFilterPhase;
import jakarta.inject.Inject;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * <b>Layer 1 — IP-based coarse rate limit on all auth endpoints.</b>
 *
 * <p>Intercepts every request to {@code /api/v1/auth/**} and enforces a
 * blanket per-IP request cap.  This stops automated scanners and
 * credential-stuffing bots before they reach controller logic.</p>
 *
 * <p>Runs <i>before</i> the SECURITY phase so abusive traffic is rejected
 * as early as possible (no JWT validation, no DB hit).</p>
 *
 * <p>Fine-grained per-email limits are enforced inside
 * {@link com.anonymous.wall.controller.AuthController}.</p>
 */
@Filter("/api/v1/auth/**")
public class AuthRateLimitFilter implements HttpServerFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    /** Max requests per IP across all auth endpoints within the window. */
    @Value("${rate-limit.auth.ip.max-requests:60}")
    private int maxRequestsPerIp;

    /** Window size in minutes. */
    @Value("${rate-limit.auth.ip.window-minutes:15}")
    private int windowMinutes;

    @Inject
    private RateLimitService rateLimitService;

    @Override
    public int getOrder() {
        // Run before SECURITY phase — reject abusive traffic early
        return ServerFilterPhase.SECURITY.order() - 10;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String clientIp = getClientIp(request);
        String key = "auth:ip:" + clientIp;

        RateLimitService.RateLimitResult result =
                rateLimitService.checkRateLimit(key, maxRequestsPerIp, Duration.ofMinutes(windowMinutes));

        if (result.isLimited()) {
            log.warn("AuthRateLimitFilter: IP rate limit hit — ip={}, path={}", clientIp, request.getPath());
            Map<String, Object> body = new HashMap<>();
            body.put("error", "Too many requests. Please try again later.");
            body.put("retryAfter", result.getRetryAfterSeconds());
            return Mono.just(
                    HttpResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                            .header("Retry-After", String.valueOf(result.getRetryAfterSeconds()))
                            .body(body)
            );
        }

        return chain.proceed(request);
    }

    // ------------------------------------------------------------------ //

    /**
     * Extract the real client IP, respecting reverse-proxy headers.
     * Order: {@code X-Forwarded-For} → {@code X-Real-IP} → socket address.
     */
    static String getClientIp(HttpRequest<?> request) {
        String xff = request.getHeaders().get("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // First entry is the original client
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeaders().get("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri.trim();
        }
        return request.getRemoteAddress().getAddress().getHostAddress();
    }
}
