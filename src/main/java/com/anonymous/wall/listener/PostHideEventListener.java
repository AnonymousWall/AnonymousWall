package com.anonymous.wall.listener;

import com.anonymous.wall.event.PostHiddenEvent;
import com.anonymous.wall.event.PostUnhiddenEvent;
import com.anonymous.wall.repository.CommentRepository;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener for post hide/unhide operations.
 * Asynchronously updates all comments when a post is hidden or unhidden.
 * 
 * This provides eventual consistency - the user gets an immediate response while
 * comments are updated in the background.
 */
@Singleton
public class PostHideEventListener implements ApplicationEventListener<Object> {
    
    private static final Logger log = LoggerFactory.getLogger(PostHideEventListener.class);
    
    @Inject
    private CommentRepository commentRepository;
    
    /**
     * Handles post hide/unhide events asynchronously.
     * Updates all comments associated with the post.
     * Uses fail-safe behavior: logs errors but doesn't throw exceptions.
     */
    @Override
    @Async
    public void onApplicationEvent(Object event) {
        if (event instanceof PostHiddenEvent postHiddenEvent) {
            handlePostHidden(postHiddenEvent);
        } else if (event instanceof PostUnhiddenEvent postUnhiddenEvent) {
            handlePostUnhidden(postUnhiddenEvent);
        }
    }
    
    private void handlePostHidden(PostHiddenEvent event) {
        log.info("Processing post hidden event: postId={}, userId={}", 
                event.getPostId(), event.getUserId());
        
        try {
            commentRepository.updateByPostId(event.getPostId(), true);
            log.debug("Hidden all comments for postId={}", event.getPostId());
        } catch (Exception e) {
            log.error("Failed to hide comments for postId={}: {}", 
                    event.getPostId(), e.getMessage(), e);
        }
        
        log.info("Post hide propagation completed for postId={}", event.getPostId());
    }
    
    private void handlePostUnhidden(PostUnhiddenEvent event) {
        log.info("Processing post unhidden event: postId={}, userId={}", 
                event.getPostId(), event.getUserId());
        
        try {
            commentRepository.updateByPostId(event.getPostId(), false);
            log.debug("Unhidden all comments for postId={}", event.getPostId());
        } catch (Exception e) {
            log.error("Failed to unhide comments for postId={}: {}", 
                    event.getPostId(), e.getMessage(), e);
        }
        
        log.info("Post unhide propagation completed for postId={}", event.getPostId());
    }
    
    @Override
    public boolean supports(Object event) {
        return event instanceof PostHiddenEvent || event instanceof PostUnhiddenEvent;
    }
}
