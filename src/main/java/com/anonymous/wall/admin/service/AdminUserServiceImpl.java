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
    
    @Override
    public Page<UserEntity> getAllUsers(Pageable pageable) {
        log.info("Admin fetching all users with pagination: page={}, size={}", 
                 pageable.getNumber(), pageable.getSize());
        return userRepository.findAll(pageable);
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
