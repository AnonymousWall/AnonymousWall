package com.anonymous.wall.admin.controller;

import com.anonymous.wall.entity.SchoolDomain;
import com.anonymous.wall.model.AdminDeleteSchoolDomain200Response;
import com.anonymous.wall.model.CreateSchoolDomainRequest;
import com.anonymous.wall.model.SchoolDomainDTO;
import com.anonymous.wall.service.SchoolDomainService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Admin controller for school domain management
 */
@Controller("/api/v1/admin/school-domains")
public class AdminSchoolDomainController {
    
    private static final Logger log = LoggerFactory.getLogger(AdminSchoolDomainController.class);
    
    @Inject
    private SchoolDomainService schoolDomainService;
    
    /**
     * Convert SchoolDomain entity to DTO
     */
    private SchoolDomainDTO mapToDTO(SchoolDomain domain) {
        SchoolDomainDTO dto = new SchoolDomainDTO();
        dto.setId(domain.getId());
        dto.setDomain(domain.getDomain());
        dto.setSchoolName(domain.getSchoolName());
        dto.setCreatedAt(domain.getCreatedAt());
        return dto;
    }
    
    /**
     * GET /admin/school-domains - List all school domains
     */
    @Get
    @Secured({"ADMIN"})
    public HttpResponse<List<SchoolDomainDTO>> getAllSchoolDomains() {
        try {
            log.info("Admin: Fetching all school domains");
            List<SchoolDomain> domains = schoolDomainService.getAllDomains();
            
            List<SchoolDomainDTO> response = new ArrayList<>();
            for (SchoolDomain domain : domains) {
                response.add(mapToDTO(domain));
            }
            
            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Error fetching school domains: {}", e.getMessage(), e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * POST /admin/school-domains - Create a new school domain
     */
    @Post
    @Secured({"ADMIN"})
    public HttpResponse<SchoolDomainDTO> createSchoolDomain(@Body CreateSchoolDomainRequest request) {
        try {
            log.info("Admin: Creating school domain '{}'", request.getDomain());
            SchoolDomain domain = schoolDomainService.createDomain(
                request.getDomain(),
                request.getSchoolName()
            );
            
            return HttpResponse.ok(mapToDTO(domain));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return HttpResponse.badRequest();
        } catch (Exception e) {
            log.error("Error creating school domain: {}", e.getMessage(), e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * DELETE /admin/school-domains/{id} - Delete a school domain
     */
    @Delete("/{id}")
    @Secured({"ADMIN"})
    public HttpResponse<AdminDeleteSchoolDomain200Response> deleteSchoolDomain(@PathVariable String id) {
        try {
            UUID domainId = UUID.fromString(id);
            
            // Check if domain exists
            Optional<SchoolDomain> domain = schoolDomainService.getDomainById(domainId);
            if (domain.isEmpty()) {
                return HttpResponse.notFound();
            }
            
            log.info("Admin: Deleting school domain '{}'", domain.get().getDomain());
            schoolDomainService.deleteDomain(domainId);
            
            AdminDeleteSchoolDomain200Response response = new AdminDeleteSchoolDomain200Response();
            response.setMessage("School domain deleted successfully");
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return HttpResponse.badRequest();
        } catch (Exception e) {
            log.error("Error deleting school domain: {}", e.getMessage(), e);
            return HttpResponse.serverError();
        }
    }
}
