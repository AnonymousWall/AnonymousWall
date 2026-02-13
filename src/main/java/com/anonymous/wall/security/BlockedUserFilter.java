package com.anonymous.wall.security;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.service.UserService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import jakarta.inject.Inject;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

/**
 * HTTP Server Filter to enforce blocked user restrictions.
 * This filter intercepts all authenticated requests and blocks access for blocked users.
 */
@Filter("/**")
public class BlockedUserFilter implements HttpServerFilter {

    private static final Logger log = LoggerFactory.getLogger(BlockedUserFilter.class);

    @Inject
    UserService userService;

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        Optional<Principal> principalOpt = request.getUserPrincipal();

        // Only check for authenticated requests
        if (principalOpt.isPresent()) {
            Principal principal = principalOpt.get();
            String principalName = principal.getName();

            try {
                UUID userId = UUID.fromString(principalName);
                Optional<UserEntity> userOpt = userService.findById(userId);

                if (userOpt.isPresent() && userOpt.get().isBlocked()) {
                    log.warn("Blocked user attempted to access: userId={}, path={}", userId, request.getPath());
                    
                    // Return 403 Forbidden for blocked users
                    MutableHttpResponse<String> response = HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body("{\"error\": \"Access denied. Your account has been blocked.\"}");
                    response.contentType("application/json");
                    return Mono.just(response);
                }
            } catch (IllegalArgumentException e) {
                // Invalid UUID format - let it pass through to be handled by other filters
                log.debug("Invalid user ID format in principal: {}", principalName);
            }
        }

        // Continue with the request if user is not blocked
        return chain.proceed(request);
    }
}
