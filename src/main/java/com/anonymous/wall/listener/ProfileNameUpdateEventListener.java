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
     */
    @Override
    @Async
    public void onApplicationEvent(ProfileNameChangedEvent event) {
        log.info("Processing profile name change event: userId={}, oldName={}, newName={}", 
                event.getUserId(), event.getOldName(), event.getNewName());
        
        try {
            // Update all posts by the user
            postRepository.updateProfileNameByUserId(event.getUserId(), event.getNewName());
            log.debug("Updated posts for userId={} with new profile name={}", 
                    event.getUserId(), event.getNewName());
            
            // Update all comments by the user
            commentRepository.updateProfileNameByUserId(event.getUserId(), event.getNewName());
            log.debug("Updated comments for userId={} with new profile name={}", 
                    event.getUserId(), event.getNewName());
            
            log.info("Profile name propagation completed for userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to propagate profile name change for userId={}: {}", 
                    event.getUserId(), e.getMessage(), e);
            // In production, you might want to add retry logic or dead letter queue here
        }
    }
}
