package com.anonymous.wall.listener;

import com.anonymous.wall.event.ProfileNameChangedEvent;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostRepository;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener for profile name changes.
 * Asynchronously updates all posts and comments when a user changes their profile name.
 * 
 * This provides eventual consistency - the user gets an immediate response while
 * posts and comments are updated in the background.
 */
@Singleton
public class ProfileNameUpdateEventListener implements ApplicationEventListener<ProfileNameChangedEvent> {
    
    private static final Logger log = LoggerFactory.getLogger(ProfileNameUpdateEventListener.class);
    
    @Inject
    private PostRepository postRepository;
    
    @Inject
    private CommentRepository commentRepository;
    
    /**
     * Handles profile name change events asynchronously.
     * Updates all posts and comments by the user with the new profile name.
     * Uses fail-safe behavior: continues processing even if one update fails.
     */
    @Override
    @Async
    public void onApplicationEvent(ProfileNameChangedEvent event) {
        log.info("Processing profile name change event: userId={}, oldName={}, newName={}", 
                event.getUserId(), event.getOldName(), event.getNewName());
        
        // Update posts (fail-safe: catch exceptions and continue)
        try {
            postRepository.updateProfileNameByUserId(event.getUserId(), event.getNewName());
            log.debug("Updated posts for userId={} with new profile name={}", 
                    event.getUserId(), event.getNewName());
        } catch (Exception e) {
            log.error("Failed to update posts for userId={}: {}", 
                    event.getUserId(), e.getMessage(), e);
        }
        
        // Update comments (fail-safe: always attempt even if posts failed)
        try {
            commentRepository.updateProfileNameByUserId(event.getUserId(), event.getNewName());
            log.debug("Updated comments for userId={} with new profile name={}", 
                    event.getUserId(), event.getNewName());
        } catch (Exception e) {
            log.error("Failed to update comments for userId={}: {}", 
                    event.getUserId(), e.getMessage(), e);
        }
        
        log.info("Profile name propagation completed for userId={}", event.getUserId());
    }
}
