package com.anonymous.wall.notification;

import com.anonymous.wall.notification.device.DeviceToken;
import com.anonymous.wall.notification.device.DeviceTokenRepository;
import com.anonymous.wall.notification.device.DeviceTokenService;
import com.anonymous.wall.notification.device.DeviceTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("DeviceController / DeviceTokenService Tests")
class DeviceControllerTest {

    private DeviceTokenRepository deviceTokenRepository;
    private DeviceTokenServiceImpl deviceTokenService;

    @BeforeEach
    void setUp() {
        deviceTokenRepository = mock(DeviceTokenRepository.class);
        deviceTokenService = new DeviceTokenServiceImpl();

        try {
            var field = DeviceTokenServiceImpl.class.getDeclaredField("deviceTokenRepository");
            field.setAccessible(true);
            field.set(deviceTokenService, deviceTokenRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("Register new token")
    class RegisterNewToken {

        @Test
        @DisplayName("Should insert new token with active=true when token does not exist")
        void registerNewTokenInsertsRecord() {
            UUID userId = UUID.randomUUID();
            String token = "new-device-token";

            when(deviceTokenRepository.findByDeviceToken(token)).thenReturn(Optional.empty());
            when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(inv -> inv.getArgument(0));

            deviceTokenService.registerToken(userId, token, "IOS");

            verify(deviceTokenRepository, times(1)).save(any(DeviceToken.class));
            verify(deviceTokenRepository, never()).update(any(DeviceToken.class));
        }

        @Test
        @DisplayName("Should save token with active=true")
        void newTokenIsActive() {
            UUID userId = UUID.randomUUID();
            String token = "new-token-active";

            when(deviceTokenRepository.findByDeviceToken(token)).thenReturn(Optional.empty());
            when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(inv -> {
                DeviceToken dt = inv.getArgument(0);
                assertTrue(dt.isActive(), "Token should be active");
                assertEquals(userId, dt.getUserId());
                assertEquals("IOS", dt.getPlatform());
                return dt;
            });

            deviceTokenService.registerToken(userId, token, "IOS");

            verify(deviceTokenRepository, times(1)).save(any(DeviceToken.class));
        }
    }

    @Nested
    @DisplayName("Register existing token — ownership reassignment")
    class RegisterExistingToken {

        @Test
        @DisplayName("Should reassign ownership to new user and set active=true")
        void existingTokenReassigned() {
            UUID originalUserId = UUID.randomUUID();
            UUID newUserId = UUID.randomUUID();
            String token = "existing-device-token";

            DeviceToken existing = new DeviceToken(originalUserId, token, "IOS");
            existing.setActive(false);

            when(deviceTokenRepository.findByDeviceToken(token)).thenReturn(Optional.of(existing));
            when(deviceTokenRepository.update(any(DeviceToken.class))).thenAnswer(inv -> inv.getArgument(0));

            deviceTokenService.registerToken(newUserId, token, "IOS");

            verify(deviceTokenRepository, times(1)).update(any(DeviceToken.class));
            verify(deviceTokenRepository, never()).save(any(DeviceToken.class));
            assertEquals(newUserId, existing.getUserId());
            assertTrue(existing.isActive());
        }
    }

    @Nested
    @DisplayName("Missing deviceToken field")
    class MissingDeviceToken {

        @Test
        @DisplayName("getActiveTokens returns empty list for user with no tokens")
        void getActiveTokensEmpty() {
            UUID userId = UUID.randomUUID();
            when(deviceTokenRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of());

            List<String> tokens = deviceTokenService.getActiveTokens(userId);

            assertTrue(tokens.isEmpty());
        }
    }

    @Nested
    @DisplayName("Deactivate token")
    class DeactivateToken {

        @Test
        @DisplayName("Should set active=false on deactivation")
        void deactivateSetsActiveFalse() {
            String token = "token-to-deactivate";
            DeviceToken dt = new DeviceToken(UUID.randomUUID(), token, "IOS");
            dt.setActive(true);

            when(deviceTokenRepository.findByDeviceToken(token)).thenReturn(Optional.of(dt));
            when(deviceTokenRepository.update(any(DeviceToken.class))).thenAnswer(inv -> inv.getArgument(0));

            deviceTokenService.deactivate(token);

            assertFalse(dt.isActive());
            verify(deviceTokenRepository, times(1)).update(dt);
        }

        @Test
        @DisplayName("Should do nothing if token not found")
        void deactivateNotFound() {
            String token = "unknown-token";
            when(deviceTokenRepository.findByDeviceToken(token)).thenReturn(Optional.empty());

            deviceTokenService.deactivate(token);

            verify(deviceTokenRepository, never()).update(any());
        }
    }
}
