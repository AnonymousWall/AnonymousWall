package com.anonymous.wall.notification.device;

import java.util.List;
import java.util.UUID;

public interface DeviceTokenService {

    void registerToken(UUID userId, String deviceToken, String platform);

    List<String> getActiveTokens(UUID userId);

    void deactivate(String deviceToken);
}
