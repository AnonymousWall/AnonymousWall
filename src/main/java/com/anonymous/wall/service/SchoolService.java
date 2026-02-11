package com.anonymous.wall.service;

import com.anonymous.wall.entity.School;
import com.anonymous.wall.entity.SchoolDomain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolService {
    /**
     * Get all schools
     */
    List<School> getAllSchools();
    
    /**
     * Get school by ID
     */
    Optional<School> getSchoolById(UUID id);
    
    /**
     * Get school by name
     */
    Optional<School> getSchoolByName(String name);
    
    /**
     * Create a new school with domains
     */
    School createSchool(String name, List<String> domains);
    
    /**
     * Delete a school and all its domains
     */
    void deleteSchool(UUID id);
    
    /**
     * Get all domains for a school
     */
    List<SchoolDomain> getSchoolDomains(UUID schoolId);
    
    /**
     * Add a domain to a school
     */
    SchoolDomain addDomainToSchool(UUID schoolId, String domain);
    
    /**
     * Remove a domain from a school
     */
    void removeDomain(UUID domainId);
    
    /**
     * Check if a domain is approved
     */
    boolean isDomainApproved(String domain);
}
