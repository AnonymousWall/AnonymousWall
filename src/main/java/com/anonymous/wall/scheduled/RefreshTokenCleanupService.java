package com.anonymous.wall.scheduled;

import com.anonymous.wall.repository.RefreshTokenRepository;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.scheduling.annotation.Scheduled;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

@Singleton
public class RefreshTokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupService.class);

    @Inject
    private RefreshTokenRepository refreshTokenRepository;

    @Scheduled(fixedDelay = "24h", initialDelay = "1m")
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Transactional
    public void purgeExpiredTokens() {
        log.info("Purging expired refresh tokens");
        long deleted = refreshTokenRepository.deleteByExpiresAtBefore(OffsetDateTime.now());
        log.info("Expired refresh token purge complete, deleted={}", deleted);
    }
}
