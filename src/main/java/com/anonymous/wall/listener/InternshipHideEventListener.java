package com.anonymous.wall.listener;

import com.anonymous.wall.event.InternshipHiddenEvent;
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
 * Event listener for internship hide operations.
 * Asynchronously updates all comments when an internship posting is hidden.
 *
 * This provides eventual consistency - the user gets an immediate response while
 * comments are updated in the background.
 */
@Singleton
public class InternshipHideEventListener implements ApplicationEventListener<InternshipHiddenEvent> {

    private static final Logger log = LoggerFactory.getLogger(InternshipHideEventListener.class);

    @Inject
    private CommentHideTransactionHelper commentHideTransactionHelper;

    /**
     * Handles internship hide events asynchronously.
     * Updates all comments associated with the internship posting.
     * Uses fail-safe behavior: logs errors but doesn't throw exceptions.
     * Runs in its own transaction to avoid closed connection issues.
     */
    @Override
    @Async(TaskExecutors.BLOCKING)
    public void onApplicationEvent(InternshipHiddenEvent event) {
        log.info("Processing internship hidden event: internshipId={}", event.getInternshipId());
        try {
            // @Transactional lives on commentService.hideByParent(), not here
            commentHideTransactionHelper.hideCommentsByParent("INTERNSHIP", event.getInternshipId(), true);
            log.info("Internship hide propagation completed for internshipId={}", event.getInternshipId());
        } catch (Exception e) {
            log.error("Failed to hide comments for internshipId={}: {}", event.getInternshipId(), e.getMessage(), e);
        }
    }
}
