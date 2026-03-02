package com.anonymous.wall.notification.device;

import io.micronaut.transaction.TransactionDefinition;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private static final Logger log = LoggerFactory.getLogger(DeviceTokenServiceImpl.class);

    @Inject
    private DeviceTokenRepository deviceTokenRepository;

    @Override
    public void registerToken(UUID userId, String deviceToken, String platform) {
        Optional<DeviceToken> existing = deviceTokenRepository.findByDeviceToken(deviceToken);
        if (existing.isPresent()) {
            DeviceToken dt = existing.get();
            dt.setUserId(userId);
            dt.setActive(true);
            dt.setUpdatedAt(OffsetDateTime.now());
            deviceTokenRepository.update(dt);
            log.info("Device token reassigned: token={}, newUserId={}", deviceToken, userId);
        } else {
            DeviceToken dt = new DeviceToken(userId, deviceToken, platform);
            deviceTokenRepository.save(dt);
            log.info("Device token registered: token={}, userId={}", deviceToken, userId);
        }
    }

    @Override
    public List<String> getActiveTokens(UUID userId) {
        return deviceTokenRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(DeviceToken::getDeviceToken)
                .collect(Collectors.toList());
    }

    @Override
    public void deactivate(String deviceToken) {
        Optional<DeviceToken> opt = deviceTokenRepository.findByDeviceToken(deviceToken);
        opt.ifPresent(dt -> {
            dt.setActive(false);
            dt.setUpdatedAt(OffsetDateTime.now());
            deviceTokenRepository.update(dt);
            log.info("Device token deactivated: token={}", deviceToken);
        });
    }
}
