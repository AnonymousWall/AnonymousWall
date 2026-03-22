package com.anonymous.wall.scheduled;

import com.anonymous.wall.repository.EmailVerificationCodeRepository;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.scheduling.annotation.Scheduled;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

/**
 * Scheduled job that purges expired email verification codes.
 *
 * Codes expire 15 minutes after creation (set in AuthServiceImpl).
 * This job runs every 6 hours to prevent the email_verification_codes
 * table from growing indefinitely.
 *
 * @see com.anonymous.wall.scheduled.RefreshTokenCleanupService analogous job for refresh tokens
 */
@Singleton
public class EmailCodeCleanupService {

    private static final Logger log = LoggerFactory.getLogger(EmailCodeCleanupService.class);

    @Inject
    private EmailVerificationCodeRepository emailCodeRepository;

    @Scheduled(fixedDelay = "6h", initialDelay = "5m")
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Transactional
    public void purgeExpiredCodes() {
        log.info("Purging expired email verification codes");
        long deleted = emailCodeRepository.deleteByExpiresAtBefore(OffsetDateTime.now());
        log.info("Expired email verification code purge complete, deleted={}", deleted);
    }
}
