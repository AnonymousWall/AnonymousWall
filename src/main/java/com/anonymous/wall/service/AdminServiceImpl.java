package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of AdminService for admin operations
 */
@Singleton
public class AdminServiceImpl implements AdminService {
    
    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);
    
    // Account status constants
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_BLOCKED = "blocked";
    public static final String STATUS_DELETED = "deleted";
    
    // Role constants
    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";
    
    @Inject
    private UserRepository userRepository;
    
    @Override
    public Page<UserEntity> getAllUsers(Pageable pageable) {
        log.info("Getting all users with pagination: page={}, size={}", 
            pageable.getNumber(), pageable.getSize());
        return userRepository.findAll(pageable);
    }
    
    @Override
    public Optional<UserEntity> getUserById(UUID userId) {
        log.info("Getting user by ID: {}", userId);
        return userRepository.findById(userId);
    }
    
    @Override
    public void blockUser(UUID userId) {
        log.info("Blocking user: {}", userId);
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        
        UserEntity user = userOpt.get();
        if (STATUS_BLOCKED.equals(user.getAccountStatus())) {
            throw new IllegalStateException("User is already blocked");
        }
        
        user.setAccountStatus(STATUS_BLOCKED);
        userRepository.update(user);
        log.info("User blocked successfully: {}", userId);
    }
    
    @Override
    public void unblockUser(UUID userId) {
        log.info("Unblocking user: {}", userId);
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        
        UserEntity user = userOpt.get();
        if (!STATUS_BLOCKED.equals(user.getAccountStatus())) {
            throw new IllegalStateException("User is not blocked");
        }
        
        user.setAccountStatus(STATUS_ACTIVE);
        userRepository.update(user);
        log.info("User unblocked successfully: {}", userId);
    }
    
    @Override
    public void deleteUser(UUID userId) {
        log.info("Soft deleting user: {}", userId);
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        
        UserEntity user = userOpt.get();
        if (STATUS_DELETED.equals(user.getAccountStatus())) {
            throw new IllegalStateException("User is already deleted");
        }
        
        user.setAccountStatus(STATUS_DELETED);
        userRepository.update(user);
        log.info("User soft deleted successfully: {}", userId);
    }
}
