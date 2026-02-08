package com.anonymous.wall.security;

import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.rules.SecurityRuleResult;
import io.micronaut.web.router.RouteMatch;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Security rule to check if the user has admin role
 */
@Singleton
public class AdminSecurityRule implements SecurityRule<HttpRequest<?>> {
    
    public static final String ADMIN_ROLE = "admin";
    
    @Override
    public Publisher<SecurityRuleResult> check(HttpRequest<?> request, RouteMatch<?> routeMatch, Authentication authentication) {
        // If user is not authenticated, deny access
        if (authentication == null) {
            return Mono.just(SecurityRuleResult.REJECTED);
        }
        
        // Check if user has admin role in their claims
        Map<String, Object> attributes = authentication.getAttributes();
        Object role = attributes.get("role");
        
        if (ADMIN_ROLE.equals(role)) {
            return Mono.just(SecurityRuleResult.ALLOWED);
        }
        
        return Mono.just(SecurityRuleResult.REJECTED);
    }
    
    @Override
    public int getOrder() {
        return -100; // Higher priority than default rules
    }
}
