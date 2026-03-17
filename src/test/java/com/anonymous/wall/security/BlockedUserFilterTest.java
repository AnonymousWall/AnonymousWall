package com.anonymous.wall.security;

import com.anonymous.wall.service.base.UserService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.filter.ServerFilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("BlockedUserFilter Tests")
class BlockedUserFilterTest {

    @Mock
    private UserService userService;

    @Mock
    private ServerFilterChain chain;

    @Mock
    private HttpRequest<?> request;

    @Mock
    private Principal principal;

    private BlockedUserFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new BlockedUserFilter(userService);
    }

    @Nested
    @DisplayName("Unauthenticated Request Tests")
    class UnauthenticatedRequestTests {

        @Test
        @DisplayName("Should allow unauthenticated requests to pass through")
        void shouldAllowUnauthenticatedRequests() {
            when(request.getPath()).thenReturn("/api/v1/posts");
            when(request.getUserPrincipal()).thenReturn(Optional.empty());
            when(chain.proceed(request)).thenReturn(Mono.empty());

            Mono.from(filter.doFilter(request, chain)).block();

            verify(chain).proceed(request);
            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("Authenticated Non-Blocked User Tests")
    class AuthenticatedNonBlockedUserTests {

        @Test
        @DisplayName("Should allow authenticated non-blocked user to pass through")
        void shouldAllowNonBlockedUser() {
            UUID userId = UUID.randomUUID();
            when(request.getPath()).thenReturn("/api/v1/posts");
            when(request.getUserPrincipal()).thenReturn(Optional.of(principal));
            when(principal.getName()).thenReturn(userId.toString());
            when(userService.isUserBlocked(userId)).thenReturn(false);
            when(chain.proceed(request)).thenReturn(Mono.empty());

            Mono.from(filter.doFilter(request, chain)).block();

            verify(userService).isUserBlocked(userId);
            verify(chain).proceed(request);
        }
    }

    @Nested
    @DisplayName("Authenticated Blocked User Tests")
    class AuthenticatedBlockedUserTests {

        @Test
        @DisplayName("Should block authenticated blocked user with 403 Forbidden")
        void shouldBlockBlockedUser() {
            UUID userId = UUID.randomUUID();
            when(request.getPath()).thenReturn("/api/v1/posts");
            when(request.getUserPrincipal()).thenReturn(Optional.of(principal));
            when(principal.getName()).thenReturn(userId.toString());
            when(userService.isUserBlocked(userId)).thenReturn(true);

            MutableHttpResponse<?> response = Mono.from(filter.doFilter(request, chain)).block();

            verify(userService).isUserBlocked(userId);
            verifyNoInteractions(chain);
            assertNotNull(response);
            assertEquals(HttpStatus.FORBIDDEN, response.getStatus());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle invalid UUID format gracefully")
        void shouldHandleInvalidUuidFormat() {
            when(request.getPath()).thenReturn("/api/v1/posts");
            when(request.getUserPrincipal()).thenReturn(Optional.of(principal));
            when(principal.getName()).thenReturn("invalid-uuid");
            when(chain.proceed(request)).thenReturn(Mono.empty());

            Mono.from(filter.doFilter(request, chain)).block();

            verify(chain).proceed(request);
            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Should handle user not found in database gracefully")
        void shouldHandleUserNotFound() {
            UUID userId = UUID.randomUUID();
            when(request.getPath()).thenReturn("/api/v1/posts");
            when(request.getUserPrincipal()).thenReturn(Optional.of(principal));
            when(principal.getName()).thenReturn(userId.toString());
            when(userService.isUserBlocked(userId)).thenReturn(false);
            when(chain.proceed(request)).thenReturn(Mono.empty());

            Mono.from(filter.doFilter(request, chain)).block();

            verify(userService).isUserBlocked(userId);
            verify(chain).proceed(request);
        }

        @Test
        @DisplayName("Should allow health check without any auth check")
        void shouldAllowHealthCheck() {
            when(request.getPath()).thenReturn("/health");
            when(chain.proceed(request)).thenReturn(Mono.empty());

            Mono.from(filter.doFilter(request, chain)).block();

            verify(chain).proceed(request);
            verifyNoInteractions(userService);
            verifyNoInteractions(principal);
        }
    }
}