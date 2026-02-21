package com.anonymous.wall.security;

import com.anonymous.wall.service.UserService;
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

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * HTTP Server Filter to enforce blocked user restrictions.
 * This filter intercepts all authenticated requests and blocks access for blocked users.
 * Runs after SECURITY phase to ensure authentication has been processed first.
 */
@Filter("/**")
public class BlockedUserFilter implements HttpServerFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(BlockedUserFilter.class);
    private static final String BLOCKED_USER_ERROR_MESSAGE = "Access denied. Your account has been blocked.";

    private final UserService userService;

    @Inject
    public BlockedUserFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    public int getOrder() {
        // Run after SECURITY phase (0) but before application logic
        return ServerFilterPhase.SECURITY.order() + 10;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        // Allow health check endpoint without authentication check
        String path = request.getPath();
        if (path.equals("/health") || path.startsWith("/health/")) {
            return chain.proceed(request);
        }

        Optional<Principal> principalOpt = request.getUserPrincipal();

        // Only check for authenticated requests
        if (principalOpt.isPresent()) {
            Principal principal = principalOpt.get();
            String principalName = principal.getName();

            try {
                UUID userId = UUID.fromString(principalName);
                
                // Use cached method to check blocked status - avoids DB hit on every request
                if (userService.isUserBlocked(userId)) {
                    log.warn("BlockedUserFilter: Blocked user attempted to access: userId={}, path={}", userId, request.getPath());
                    
                    // Return 403 Forbidden for blocked users
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("error", BLOCKED_USER_ERROR_MESSAGE);
                    
                    MutableHttpResponse<Map<String, String>> response = HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(errorResponse);
                    return Mono.just(response);
                }
                
                log.debug("BlockedUserFilter: User {} is not blocked, allowing access to path: {}", userId, request.getPath());
            } catch (IllegalArgumentException e) {
                // Invalid UUID format - let it pass through to be handled by other filters
                log.debug("BlockedUserFilter: Invalid user ID format in principal: {}", principalName);
            }
        } else {
            log.debug("BlockedUserFilter: No authenticated principal for path: {}", request.getPath());
        }

        // Continue with the request if user is not blocked
        return chain.proceed(request);
    }
}
