package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.UserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

@Singleton
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Inject
    private UserRepository userRepository;

    /**
     * Find user by ID
     */
    @Override
    public Optional<UserEntity> findById(UUID userId) {
        log.debug("Finding user by ID: {}", userId);
        return userRepository.findById(userId);
    }

    /**
     * Find user by email
     */
    @Override
    public Optional<UserEntity> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Update user profile name
     */
    @Override
    public UserEntity updateProfileName(UUID userId, String profileName) {
        log.debug("Updating profile name for user: {}", userId);
        
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", userId);
            throw new IllegalArgumentException("User not found");
        }

        UserEntity user = userOpt.get();
        
        // Validate and set profile name
        String newProfileName = profileName;
        if (newProfileName == null || newProfileName.trim().isEmpty()) {
            newProfileName = "Anonymous";
        }
        
        user.setProfileName(newProfileName.trim());
        UserEntity updated = userRepository.update(user);
        
        log.info("Profile name updated for user: {}, newName={}", userId, newProfileName.trim());
        return updated;
    }

    /**
     * Update user entity
     */
    @Override
    public UserEntity update(UserEntity user) {
        log.debug("Updating user: {}", user.getId());
        return userRepository.update(user);
    }

    /**
     * Save user entity
     */
    @Override
    public UserEntity save(UserEntity user) {
        log.debug("Saving user");
        return userRepository.save(user);
    }
}
