package com.anonymous.wall.service;

import com.anonymous.wall.entity.SchoolDomain;
import com.anonymous.wall.repository.SchoolDomainRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
public class SchoolDomainServiceImpl implements SchoolDomainService {

    @Inject
    private SchoolDomainRepository schoolDomainRepository;

    @Override
    public List<SchoolDomain> getAllDomains() {
        return StreamSupport.stream(schoolDomainRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SchoolDomain> getDomainById(UUID id) {
        return schoolDomainRepository.findById(id);
    }

    @Override
    @Transactional
    public SchoolDomain createDomain(String domain, String schoolName) {
        // Check if domain already exists
        String normalizedDomain = domain.toLowerCase().trim();
        if (schoolDomainRepository.existsByDomain(normalizedDomain)) {
            throw new IllegalArgumentException("Domain '" + normalizedDomain + "' already exists");
        }

        SchoolDomain schoolDomain = new SchoolDomain();
        schoolDomain.setDomain(normalizedDomain);
        schoolDomain.setSchoolName(schoolName.trim());
        schoolDomain.setCreatedAt(OffsetDateTime.now());
        return schoolDomainRepository.save(schoolDomain);
    }

    @Override
    @Transactional
    public void deleteDomain(UUID id) {
        if (!schoolDomainRepository.existsById(id)) {
            throw new IllegalArgumentException("School domain not found");
        }
        schoolDomainRepository.deleteById(id);
    }

    @Override
    public boolean isDomainApproved(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }
        String normalizedDomain = domain.toLowerCase().trim();
        return schoolDomainRepository.existsByDomain(normalizedDomain);
    }
}
