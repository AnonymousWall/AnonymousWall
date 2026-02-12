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
     * Note: When the 'blocked' filter is active, only basic sorting is supported (createdAt).
     * For advanced sorting options, omit the 'blocked' filter parameter.
     * 
     * @param pageable Pagination parameters
     * @param blocked Filter by blocked status (null = all users)
     * @param sortBy Sort field (case-insensitive): "createdAt", "schoolDomain", "reportCount", "postCount", "commentCount"
     * @param sortOrder Sort order (case-insensitive): "asc" or "desc" (default: desc)
     * @return Page of users matching the criteria
     */
    @Override
    public Page<UserEntity> getAllUsers(Pageable pageable, Boolean blocked, String sortBy, String sortOrder) {
        log.info("Admin fetching users - page={}, size={}, blocked={}, sortBy={}, sortOrder={}", 
                 pageable.getNumber(), pageable.getSize(), blocked, sortBy, sortOrder);
        
        // Determine sort order (default to desc)
        boolean isDesc = sortOrder == null || sortOrder.equalsIgnoreCase("desc");
        
        // Handle filtering by blocked status
        if (blocked != null) {
            if (sortBy == null || sortBy.equalsIgnoreCase("createdAt")) {
                return isDesc ? 
                    userRepository.findByBlockedOrderByCreatedAtDesc(blocked, pageable) :
                    userRepository.findByBlockedOrderByCreatedAtAsc(blocked, pageable);
            }
            // For other sort options with blocked filter, just filter and use default sort
            return userRepository.findByBlocked(blocked, pageable);
        }
        
        // Handle sorting without filtering
        if (sortBy == null) {
            return userRepository.findAll(pageable);
        }
        
        switch (sortBy.toLowerCase()) {
            case "createdat":
                return isDesc ? 
                    userRepository.findAllOrderByCreatedAtDesc(pageable) :
                    userRepository.findAllOrderByCreatedAtAsc(pageable);
            
            case "schooldomain":
                return userRepository.findAllOrderBySchoolDomain(pageable);
            
            case "reportcount":
                return isDesc ?
                    userRepository.findAllOrderByReportCountDesc(pageable) :
                    userRepository.findAllOrderByReportCountAsc(pageable);
            
            case "postcount":
                return isDesc ?
                    userRepository.findAllOrderByPostCountDesc(pageable) :
                    userRepository.findAllOrderByPostCountAsc(pageable);
            
            case "commentcount":
                return isDesc ?
                    userRepository.findAllOrderByCommentCountDesc(pageable) :
                    userRepository.findAllOrderByCommentCountAsc(pageable);
            
            default:
                return userRepository.findAll(pageable);
        }
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
