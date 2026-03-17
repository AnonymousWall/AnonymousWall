package com.anonymous.wall.listener;

import com.anonymous.wall.event.PostHiddenEvent;
import com.anonymous.wall.listener.helper.CommentHideTransactionHelper;
import com.anonymous.wall.service.base.CommentsService;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.Async;
import io.micronaut.transaction.TransactionDefinition;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener for post hide operations.
 * Asynchronously updates all comments when a post is hidden.
 * 
 * This provides eventual consistency - the user gets an immediate response while
 * comments are updated in the background.
 */
@Singleton
public class PostHideEventListener implements ApplicationEventListener<PostHiddenEvent> {

    private static final Logger log = LoggerFactory.getLogger(PostHideEventListener.class);

    @Inject
    private CommentHideTransactionHelper commentHideTransactionHelper;

    @Override
    @Async(TaskExecutors.BLOCKING)
    public void onApplicationEvent(PostHiddenEvent event) {
        log.info("Processing post hidden event: postId={}, userId={}",
                event.getPostId(), event.getUserId());

        try {
            commentHideTransactionHelper.hideCommentsByParent("POST", event.getPostId(), true);
            log.info("Post hide propagation completed for postId={}", event.getPostId());
        } catch (Exception e) {
            log.error("Failed to hide comments for postId={}: {}",
                    event.getPostId(), e.getMessage(), e);
        }
    }
}