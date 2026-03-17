package com.anonymous.wall.service.retry;

import com.anonymous.wall.entity.SchoolDomain;
import com.anonymous.wall.service.base.SchoolDomainService;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * School domain retry wrapper.
 */
@Singleton
public class SchoolDomainRetryService {

    private final SchoolDomainService schoolDomainService;

    public SchoolDomainRetryService(SchoolDomainService schoolDomainService) {
        this.schoolDomainService = schoolDomainService;
    }

    @Retryable(attempts = "3", delay = "500ms")
    public List<SchoolDomain> getAllDomains() {
        return schoolDomainService.getAllDomains();
    }

    @Retryable(attempts = "3", delay = "500ms")
    public Optional<SchoolDomain> getDomainById(UUID id) {
        return schoolDomainService.getDomainById(id);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public SchoolDomain createDomain(String domain, String schoolName) {
        return schoolDomainService.createDomain(domain, schoolName);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public void deleteDomain(UUID id) {
        schoolDomainService.deleteDomain(id);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public boolean isDomainApproved(String domain) {
        return schoolDomainService.isDomainApproved(domain);
    }
}
