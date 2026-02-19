package com.anonymous.wall.listener;

import com.anonymous.wall.event.ProfileNameChangedEvent;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.InternshipRepository;
import com.anonymous.wall.repository.MarketplaceItemRepository;
import com.anonymous.wall.repository.PostRepository;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.scheduling.annotation.Async;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ProfileNameUpdateEventListener implements ApplicationEventListener<ProfileNameChangedEvent> {
    
    private static final Logger log = LoggerFactory.getLogger(ProfileNameUpdateEventListener.class);
    
    @Inject
    private PostRepository postRepository;
    
    @Inject
    private CommentRepository commentRepository;

    @Inject
    private InternshipRepository internshipRepository;

    @Inject
    private MarketplaceItemRepository marketplaceItemRepository;
    
    @Override
    @Async
    @Transactional
    public void onApplicationEvent(ProfileNameChangedEvent event) {
        log.info("Processing profile name change event: userId={}, oldName={}, newName={}", 
                event.getUserId(), event.getOldName(), event.getNewName());
        
        try {
            postRepository.updateProfileNameByUserId(event.getUserId(), event.getNewName());
            log.debug("Updated posts for userId={} with new profile name={}", 
                    event.getUserId(), event.getNewName());
        } catch (Exception e) {
            log.error("Failed to update posts for userId={}: {}", 
                    event.getUserId(), e.getMessage(), e);
        }
        
        try {
            commentRepository.updateProfileNameByUserId(event.getUserId(), event.getNewName());
            log.debug("Updated comments for userId={} with new profile name={}", 
                    event.getUserId(), event.getNewName());
        } catch (Exception e) {
            log.error("Failed to update comments for userId={}: {}", 
                    event.getUserId(), e.getMessage(), e);
        }

        try {
            internshipRepository.updateProfileNameByUserId(event.getUserId(), event.getNewName());
            log.debug("Updated internships for userId={} with new profile name={}", 
                    event.getUserId(), event.getNewName());
        } catch (Exception e) {
            log.error("Failed to update internships for userId={}: {}", 
                    event.getUserId(), e.getMessage(), e);
        }

        try {
            marketplaceItemRepository.updateProfileNameByUserId(event.getUserId(), event.getNewName());
            log.debug("Updated marketplace items for userId={} with new profile name={}", 
                    event.getUserId(), event.getNewName());
        } catch (Exception e) {
            log.error("Failed to update marketplace items for userId={}: {}", 
                    event.getUserId(), e.getMessage(), e);
        }
        
        log.info("Profile name propagation completed for userId={}", event.getUserId());
    }
}
