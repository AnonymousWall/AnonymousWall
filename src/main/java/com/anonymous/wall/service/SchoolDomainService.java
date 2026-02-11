package com.anonymous.wall.service;

import com.anonymous.wall.entity.SchoolDomain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolDomainService {
    /**
     * Get all school domains
     */
    List<SchoolDomain> getAllDomains();
    
    /**
     * Get school domain by ID
     */
    Optional<SchoolDomain> getDomainById(UUID id);
    
    /**
     * Create a new school domain
     */
    SchoolDomain createDomain(String domain, String schoolName);
    
    /**
     * Delete a school domain
     */
    void deleteDomain(UUID id);
    
    /**
     * Check if a domain is approved
     */
    boolean isDomainApproved(String domain);
}
