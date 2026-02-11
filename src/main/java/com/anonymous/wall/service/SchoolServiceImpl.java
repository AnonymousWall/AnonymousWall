package com.anonymous.wall.service;

import com.anonymous.wall.entity.School;
import com.anonymous.wall.entity.SchoolDomain;
import com.anonymous.wall.repository.SchoolDomainRepository;
import com.anonymous.wall.repository.SchoolRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
public class SchoolServiceImpl implements SchoolService {

    @Inject
    private SchoolRepository schoolRepository;

    @Inject
    private SchoolDomainRepository schoolDomainRepository;

    @Override
    public List<School> getAllSchools() {
        return StreamSupport.stream(schoolRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<School> getSchoolById(UUID id) {
        return schoolRepository.findById(id);
    }

    @Override
    public Optional<School> getSchoolByName(String name) {
        return schoolRepository.findByName(name);
    }

    @Override
    @Transactional
    public School createSchool(String name, List<String> domains) {
        // Check if school already exists
        Optional<School> existing = schoolRepository.findByName(name);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("School with name '" + name + "' already exists");
        }

        // Create school
        School school = new School();
        school.setName(name);
        school = schoolRepository.save(school);

        // Add domains
        for (String domain : domains) {
            String normalizedDomain = domain.toLowerCase().trim();
            
            // Check if domain already exists
            if (schoolDomainRepository.existsByDomain(normalizedDomain)) {
                throw new IllegalArgumentException("Domain '" + normalizedDomain + "' already exists");
            }
            
            SchoolDomain schoolDomain = new SchoolDomain();
            schoolDomain.setSchoolId(school.getId());
            schoolDomain.setDomain(normalizedDomain);
            schoolDomainRepository.save(schoolDomain);
        }

        return school;
    }

    @Override
    @Transactional
    public void deleteSchool(UUID id) {
        if (!schoolRepository.existsById(id)) {
            throw new IllegalArgumentException("School not found");
        }
        
        // Domains will be deleted automatically due to CASCADE constraint
        schoolRepository.deleteById(id);
    }

    @Override
    public List<SchoolDomain> getSchoolDomains(UUID schoolId) {
        return schoolDomainRepository.findBySchoolId(schoolId);
    }

    @Override
    @Transactional
    public SchoolDomain addDomainToSchool(UUID schoolId, String domain) {
        // Check if school exists
        if (!schoolRepository.existsById(schoolId)) {
            throw new IllegalArgumentException("School not found");
        }

        String normalizedDomain = domain.toLowerCase().trim();
        
        // Check if domain already exists
        if (schoolDomainRepository.existsByDomain(normalizedDomain)) {
            throw new IllegalArgumentException("Domain '" + normalizedDomain + "' already exists");
        }

        SchoolDomain schoolDomain = new SchoolDomain();
        schoolDomain.setSchoolId(schoolId);
        schoolDomain.setDomain(normalizedDomain);
        return schoolDomainRepository.save(schoolDomain);
    }

    @Override
    @Transactional
    public void removeDomain(UUID domainId) {
        if (!schoolDomainRepository.existsById(domainId)) {
            throw new IllegalArgumentException("Domain not found");
        }
        schoolDomainRepository.deleteById(domainId);
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
