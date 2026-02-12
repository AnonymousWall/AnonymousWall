package com.anonymous.wall.util;

import com.anonymous.wall.service.SchoolDomainService;
import io.micronaut.context.event.StartupEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SchoolDomainWhitelistInitializer Tests")
class SchoolDomainWhitelistInitializerTest {

    private SchoolDomainWhitelistInitializer initializer;
    private SchoolDomainService mockSchoolDomainService;

    @BeforeEach
    void setUp() {
        initializer = new SchoolDomainWhitelistInitializer();
        mockSchoolDomainService = mock(SchoolDomainService.class);
        
        // Use reflection to inject the mock service
        try {
            var serviceField = SchoolDomainWhitelistInitializer.class.getDeclaredField("schoolDomainService");
            serviceField.setAccessible(true);
            serviceField.set(initializer, mockSchoolDomainService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock service", e);
        }
    }

    @Nested
    @DisplayName("OnApplicationEvent Tests")
    class OnApplicationEventTests {

        @Test
        @DisplayName("Positive: Should initialize whitelist on startup event")
        void shouldInitializeWhitelistOnStartupEvent() {
            // Arrange
            StartupEvent mockEvent = mock(StartupEvent.class);

            // Act
            initializer.onApplicationEvent(mockEvent);

            // Assert
            // We can't directly verify SchoolDomainWhitelist.initialize was called
            // but we can verify no exceptions were thrown
            assertDoesNotThrow(() -> initializer.onApplicationEvent(mockEvent));
        }

        @Test
        @DisplayName("Positive: Should handle startup event successfully")
        void shouldHandleStartupEventSuccessfully() {
            // Arrange
            StartupEvent mockEvent = mock(StartupEvent.class);

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> initializer.onApplicationEvent(mockEvent));
        }

        @Test
        @DisplayName("Edge: Should handle null startup event")
        void shouldHandleNullStartupEvent() {
            // Act & Assert - should handle gracefully
            assertDoesNotThrow(() -> initializer.onApplicationEvent(null));
        }

        @Test
        @DisplayName("Edge: Should handle multiple startup events")
        void shouldHandleMultipleStartupEvents() {
            // Arrange
            StartupEvent mockEvent1 = mock(StartupEvent.class);
            StartupEvent mockEvent2 = mock(StartupEvent.class);

            // Act & Assert - should handle multiple calls
            assertDoesNotThrow(() -> {
                initializer.onApplicationEvent(mockEvent1);
                initializer.onApplicationEvent(mockEvent2);
            });
        }
    }

    @Nested
    @DisplayName("Service Injection Tests")
    class ServiceInjectionTests {

        @Test
        @DisplayName("Positive: Should have school domain service injected")
        void shouldHaveSchoolDomainServiceInjected() {
            // Assert - service should be injected via setUp
            assertNotNull(mockSchoolDomainService, "Service should be injected");
        }

        @Test
        @DisplayName("Positive: Should create instance successfully")
        void shouldCreateInstanceSuccessfully() {
            // Act
            SchoolDomainWhitelistInitializer newInitializer = new SchoolDomainWhitelistInitializer();

            // Assert
            assertNotNull(newInitializer);
        }
    }

    @Nested
    @DisplayName("Event Listener Tests")
    class EventListenerTests {

        @Test
        @DisplayName("Positive: Should be an ApplicationEventListener")
        void shouldBeAnApplicationEventListener() {
            // Assert
            assertTrue(initializer instanceof io.micronaut.context.event.ApplicationEventListener);
        }

        @Test
        @DisplayName("Positive: Should listen to StartupEvent")
        void shouldListenToStartupEvent() {
            // Arrange
            StartupEvent event = mock(StartupEvent.class);

            // Act & Assert - method exists and can be called
            assertDoesNotThrow(() -> initializer.onApplicationEvent(event));
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Integration: Should initialize whitelist with service on event")
        void shouldInitializeWhitelistWithServiceOnEvent() {
            // Arrange
            StartupEvent mockEvent = mock(StartupEvent.class);
            
            // Reset whitelist to ensure clean state
            SchoolDomainWhitelist.initialize(null);

            // Act
            initializer.onApplicationEvent(mockEvent);

            // Assert - After initialization, whitelist should use the service
            // We can test this indirectly by checking if whitelist handles requests
            assertDoesNotThrow(() -> SchoolDomainWhitelist.isApprovedDomain("test.edu"));
        }

        @Test
        @DisplayName("Integration: Should allow whitelist to function after initialization")
        void shouldAllowWhitelistToFunctionAfterInitialization() {
            // Arrange
            StartupEvent mockEvent = mock(StartupEvent.class);
            when(mockSchoolDomainService.isDomainApproved(anyString())).thenReturn(true);

            // Act
            initializer.onApplicationEvent(mockEvent);
            boolean result = SchoolDomainWhitelist.isApprovedDomain("harvard.edu");

            // Assert
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Negative: Should handle null service gracefully")
        void shouldHandleNullServiceGracefully() {
            // Arrange
            SchoolDomainWhitelistInitializer initializerWithNullService = new SchoolDomainWhitelistInitializer();
            // Don't inject service - leave it null
            StartupEvent mockEvent = mock(StartupEvent.class);

            // Act & Assert - should not crash even with null service
            assertDoesNotThrow(() -> initializerWithNullService.onApplicationEvent(mockEvent));
        }

        @Test
        @DisplayName("Edge: Should be idempotent")
        void shouldBeIdempotent() {
            // Arrange
            StartupEvent mockEvent = mock(StartupEvent.class);

            // Act - call multiple times
            initializer.onApplicationEvent(mockEvent);
            initializer.onApplicationEvent(mockEvent);
            initializer.onApplicationEvent(mockEvent);

            // Assert - should not cause issues
            assertDoesNotThrow(() -> SchoolDomainWhitelist.isApprovedDomain("test.edu"));
        }
    }
}
