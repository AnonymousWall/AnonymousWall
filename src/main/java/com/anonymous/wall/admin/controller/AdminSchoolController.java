package com.anonymous.wall.admin.controller;

import com.anonymous.wall.entity.School;
import com.anonymous.wall.entity.SchoolDomain;
import com.anonymous.wall.service.SchoolService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Admin controller for school management
 */
@Controller("/api/v1/admin/schools")
public class AdminSchoolController {
    
    private static final Logger log = LoggerFactory.getLogger(AdminSchoolController.class);
    
    @Inject
    private SchoolService schoolService;
    
    /**
     * GET /admin/schools - List all schools
     */
    @Get
    @Secured({"ADMIN"})
    public HttpResponse<Object> getAllSchools() {
        try {
            log.info("Admin: Fetching all schools");
            List<School> schools = schoolService.getAllSchools();
            
            // Build response with domains
            List<Map<String, Object>> response = new ArrayList<>();
            for (School school : schools) {
                Map<String, Object> schoolData = new HashMap<>();
                schoolData.put("id", school.getId());
                schoolData.put("name", school.getName());
                schoolData.put("createdAt", school.getCreatedAt());
                
                List<SchoolDomain> domains = schoolService.getSchoolDomains(school.getId());
                List<Map<String, Object>> domainList = new ArrayList<>();
                for (SchoolDomain domain : domains) {
                    Map<String, Object> domainData = new HashMap<>();
                    domainData.put("id", domain.getId());
                    domainData.put("domain", domain.getDomain());
                    domainData.put("createdAt", domain.getCreatedAt());
                    domainList.add(domainData);
                }
                schoolData.put("domains", domainList);
                
                response.add(schoolData);
            }
            
            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Error fetching schools: {}", e.getMessage(), e);
            return HttpResponse.serverError(Map.of("error", "Failed to fetch schools"));
        }
    }
    
    /**
     * GET /admin/schools/{id} - Get school by ID
     */
    @Get("/{id}")
    @Secured({"ADMIN"})
    public HttpResponse<Object> getSchoolById(@PathVariable String id) {
        try {
            UUID schoolId = UUID.fromString(id);
            Optional<School> school = schoolService.getSchoolById(schoolId);
            
            if (school.isEmpty()) {
                return HttpResponse.notFound(Map.of("error", "School not found"));
            }
            
            School s = school.get();
            List<SchoolDomain> domains = schoolService.getSchoolDomains(schoolId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", s.getId());
            response.put("name", s.getName());
            response.put("createdAt", s.getCreatedAt());
            
            List<Map<String, Object>> domainList = new ArrayList<>();
            for (SchoolDomain domain : domains) {
                Map<String, Object> domainData = new HashMap<>();
                domainData.put("id", domain.getId());
                domainData.put("domain", domain.getDomain());
                domainData.put("createdAt", domain.getCreatedAt());
                domainList.add(domainData);
            }
            response.put("domains", domainList);
            
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(Map.of("error", "Invalid school ID"));
        } catch (Exception e) {
            log.error("Error fetching school: {}", e.getMessage(), e);
            return HttpResponse.serverError(Map.of("error", "Failed to fetch school"));
        }
    }
    
    /**
     * POST /admin/schools - Create a new school with domains
     * Request body: { "name": "School Name", "domains": ["domain1.edu", "domain2.edu"] }
     */
    @Post
    @Secured({"ADMIN"})
    public HttpResponse<Object> createSchool(@Body Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            @SuppressWarnings("unchecked")
            List<String> domains = (List<String>) request.get("domains");
            
            if (name == null || name.trim().isEmpty()) {
                return HttpResponse.badRequest(Map.of("error", "School name is required"));
            }
            
            if (domains == null || domains.isEmpty()) {
                return HttpResponse.badRequest(Map.of("error", "At least one domain is required"));
            }
            
            log.info("Admin: Creating school '{}' with {} domains", name, domains.size());
            School school = schoolService.createSchool(name.trim(), domains);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", school.getId());
            response.put("name", school.getName());
            response.put("createdAt", school.getCreatedAt());
            response.put("message", "School created successfully");
            
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating school: {}", e.getMessage(), e);
            return HttpResponse.serverError(Map.of("error", "Failed to create school"));
        }
    }
    
    /**
     * DELETE /admin/schools/{id} - Delete a school and all its domains
     */
    @Delete("/{id}")
    @Secured({"ADMIN"})
    public HttpResponse<Object> deleteSchool(@PathVariable String id) {
        try {
            UUID schoolId = UUID.fromString(id);
            
            Optional<School> school = schoolService.getSchoolById(schoolId);
            if (school.isEmpty()) {
                return HttpResponse.notFound(Map.of("error", "School not found"));
            }
            
            log.info("Admin: Deleting school '{}'", school.get().getName());
            schoolService.deleteSchool(schoolId);
            
            return HttpResponse.ok(Map.of("message", "School deleted successfully"));
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting school: {}", e.getMessage(), e);
            return HttpResponse.serverError(Map.of("error", "Failed to delete school"));
        }
    }
    
    /**
     * POST /admin/schools/{id}/domains - Add a domain to a school
     * Request body: { "domain": "newdomain.edu" }
     */
    @Post("/{id}/domains")
    @Secured({"ADMIN"})
    public HttpResponse<Object> addDomain(@PathVariable String id, @Body Map<String, String> request) {
        try {
            UUID schoolId = UUID.fromString(id);
            String domain = request.get("domain");
            
            if (domain == null || domain.trim().isEmpty()) {
                return HttpResponse.badRequest(Map.of("error", "Domain is required"));
            }
            
            log.info("Admin: Adding domain '{}' to school", domain);
            SchoolDomain schoolDomain = schoolService.addDomainToSchool(schoolId, domain.trim());
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", schoolDomain.getId());
            response.put("domain", schoolDomain.getDomain());
            response.put("createdAt", schoolDomain.getCreatedAt());
            response.put("message", "Domain added successfully");
            
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error adding domain: {}", e.getMessage(), e);
            return HttpResponse.serverError(Map.of("error", "Failed to add domain"));
        }
    }
    
    /**
     * DELETE /admin/schools/domains/{domainId} - Remove a domain
     */
    @Delete("/domains/{domainId}")
    @Secured({"ADMIN"})
    public HttpResponse<Object> removeDomain(@PathVariable String domainId) {
        try {
            UUID id = UUID.fromString(domainId);
            
            log.info("Admin: Removing domain with ID {}", id);
            schoolService.removeDomain(id);
            
            return HttpResponse.ok(Map.of("message", "Domain removed successfully"));
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error removing domain: {}", e.getMessage(), e);
            return HttpResponse.serverError(Map.of("error", "Failed to remove domain"));
        }
    }
}
