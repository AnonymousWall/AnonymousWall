package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Implementation of admin user management service
 */
@Singleton
public class AdminUserServiceImpl implements AdminUserService {
    
    private static final Logger log = LoggerFactory.getLogger(AdminUserServiceImpl.class);
    
    @Inject
    private UserRepository userRepository;
    
    /**
     * Get all users with pagination and optional filters/sorting.
     * 
     * @param pageable Pagination parameters
     * @param blocked Filter by blocked status (null = all users)
     * @param sortBy Sort field (case-insensitive): "createdAt", "schoolDomain", "reportCount"
     * @param sortOrder Sort order (case-insensitive): "asc" or "desc" (default: desc)
     * @return Page of users matching the criteria
     */
    @Override
    public Page<UserEntity> getAllUsers(Pageable pageable, Boolean blocked, String sortBy, String sortOrder) {
        log.info("Admin fetching users - page={}, size={}, blocked={}, sortBy={}, sortOrder={}", 
                 pageable.getNumber(), pageable.getSize(), blocked, sortBy, sortOrder);
        if (sortOrder == null || sortOrder.isEmpty()) {
            sortOrder = "desc"; // default to descending
        }

        // Determine sort order (default to desc)
        boolean isDesc = sortOrder.equalsIgnoreCase("desc");
        
        // Case 1: No filters, no custom sorting - return all with default pagination
        // Note: Without explicit ORDER BY, the result order is database-dependent and not guaranteed
        if (blocked == null && sortBy == null) {
            return userRepository.findAll(pageable);
        }
        
        // Case 2: No filters, but custom sorting specified - use sorting methods
        if (blocked == null && sortBy != null) {
            switch (sortBy.toLowerCase()) {
                case "createdat":
                    return isDesc ? 
                        userRepository.findAllOrderByCreatedAtDesc(pageable) :
                        userRepository.findAllOrderByCreatedAtAsc(pageable);
                
                case "schooldomain":
                    return isDesc ?
                        userRepository.findAllOrderBySchoolDomainDesc(pageable) :
                        userRepository.findAllOrderBySchoolDomainAsc(pageable);
                
                case "reportcount":
                    return isDesc ?
                        userRepository.findAllOrderByReportCountDesc(pageable) :
                        userRepository.findAllOrderByReportCountAsc(pageable);
                
                default:
                    return userRepository.findAll(pageable);
            }
        }
        
        // Case 3: Filter by blocked status (with or without sorting)
        // For blocked filter, we support createdAt sorting via dedicated repository methods.
        // Other sort fields are not supported with blocked filter due to lack of corresponding
        // repository methods (would need findByBlockedOrderBySchoolDomain, findByBlockedOrderByReportCount, etc.)
        // When other sorts are requested, result order is database-dependent without explicit ORDER BY.
        if (sortBy == null || sortBy.equalsIgnoreCase("createdAt")) {
            return isDesc ? 
                userRepository.findByBlockedOrderByCreatedAtDesc(blocked, pageable) :
                userRepository.findByBlockedOrderByCreatedAtAsc(blocked, pageable);
        }
        
        // For other sort options with blocked filter, order is database-dependent
        log.warn("sortBy parameter '{}' is not fully supported with blocked filter and will use database-dependent ordering. " +
                 "Only 'createdAt' sorting is supported with blocked filter.", sortBy);
        return userRepository.findByBlocked(blocked, pageable);
    }
    
    @Override
    public UserEntity getUserById(UUID userId) {
        log.info("Admin fetching user by ID: {}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }
    
    @Override
    public void blockUser(UUID userId) {
        log.info("Admin blocking user: {}", userId);
        UserEntity user = getUserById(userId);
        user.setBlocked(true);
        userRepository.update(user);
        log.info("User blocked successfully: {}", userId);
    }
    
    @Override
    public void unblockUser(UUID userId) {
        log.info("Admin unblocking user: {}", userId);
        UserEntity user = getUserById(userId);
        user.setBlocked(false);
        userRepository.update(user);
        log.info("User unblocked successfully: {}", userId);
    }
}
