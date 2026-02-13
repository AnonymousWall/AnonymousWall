package com.anonymous.wall.security;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.service.UserService;
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
import org.reactivestreams.Publisher;
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
        filter = new BlockedUserFilter();
        filter.userService = userService;
    }

    @Nested
    @DisplayName("Unauthenticated Request Tests")
    class UnauthenticatedRequestTests {

        @Test
        @DisplayName("Should allow unauthenticated requests to pass through")
        void shouldAllowUnauthenticatedRequests() {
            // Arrange
            when(request.getUserPrincipal()).thenReturn(Optional.empty());
            when(chain.proceed(request)).thenReturn(Mono.empty());

            // Act
            Publisher<MutableHttpResponse<?>> result = filter.doFilter(request, chain);

            // Assert
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
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = new UserEntity();
            user.setId(userId);
            user.setBlocked(false);

            when(request.getUserPrincipal()).thenReturn(Optional.of(principal));
            when(principal.getName()).thenReturn(userId.toString());
            when(userService.findById(userId)).thenReturn(Optional.of(user));
            when(chain.proceed(request)).thenReturn(Mono.empty());

            // Act
            Publisher<MutableHttpResponse<?>> result = filter.doFilter(request, chain);

            // Assert
            verify(chain).proceed(request);
            verify(userService).findById(userId);
        }
    }

    @Nested
    @DisplayName("Authenticated Blocked User Tests")
    class AuthenticatedBlockedUserTests {

        @Test
        @DisplayName("Should block authenticated blocked user with 403 Forbidden")
        void shouldBlockBlockedUser() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = new UserEntity();
            user.setId(userId);
            user.setBlocked(true);

            when(request.getUserPrincipal()).thenReturn(Optional.of(principal));
            when(principal.getName()).thenReturn(userId.toString());
            when(request.getPath()).thenReturn("/api/v1/posts");
            when(userService.findById(userId)).thenReturn(Optional.of(user));

            // Act
            Publisher<MutableHttpResponse<?>> result = filter.doFilter(request, chain);

            // Assert
            verify(userService).findById(userId);
            verifyNoInteractions(chain);
            
            // Verify that response is 403
            Mono.from(result).subscribe(response -> {
                assertEquals(HttpStatus.FORBIDDEN, response.getStatus());
                assertTrue(response.getBody(String.class).isPresent());
                assertTrue(response.getBody(String.class).get().contains("blocked"));
            });
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle invalid UUID format gracefully")
        void shouldHandleInvalidUuidFormat() {
            // Arrange
            when(request.getUserPrincipal()).thenReturn(Optional.of(principal));
            when(principal.getName()).thenReturn("invalid-uuid");
            when(chain.proceed(request)).thenReturn(Mono.empty());

            // Act
            Publisher<MutableHttpResponse<?>> result = filter.doFilter(request, chain);

            // Assert
            verify(chain).proceed(request);
            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Should handle user not found in database gracefully")
        void shouldHandleUserNotFound() {
            // Arrange
            UUID userId = UUID.randomUUID();
            when(request.getUserPrincipal()).thenReturn(Optional.of(principal));
            when(principal.getName()).thenReturn(userId.toString());
            when(userService.findById(userId)).thenReturn(Optional.empty());
            when(chain.proceed(request)).thenReturn(Mono.empty());

            // Act
            Publisher<MutableHttpResponse<?>> result = filter.doFilter(request, chain);

            // Assert
            verify(userService).findById(userId);
            verify(chain).proceed(request);
        }
    }
}
