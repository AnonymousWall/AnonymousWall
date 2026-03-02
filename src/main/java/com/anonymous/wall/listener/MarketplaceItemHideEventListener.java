package com.anonymous.wall.listener;

import com.anonymous.wall.event.MarketplaceItemHiddenEvent;
import com.anonymous.wall.repository.CommentRepository;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.scheduling.annotation.Async;
import io.micronaut.transaction.TransactionDefinition;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener for marketplace item hide operations.
 * Asynchronously updates all comments when a marketplace item is hidden.
 *
 * This provides eventual consistency - the user gets an immediate response while
 * comments are updated in the background.
 */
@Singleton
public class MarketplaceItemHideEventListener implements ApplicationEventListener<MarketplaceItemHiddenEvent> {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceItemHideEventListener.class);

    @Inject
    private CommentRepository commentRepository;

    /**
     * Handles marketplace item hide events asynchronously.
     * Updates all comments associated with the marketplace item.
     * Uses fail-safe behavior: logs errors but doesn't throw exceptions.
     * Runs in its own transaction to avoid closed connection issues.
     */
    @Override
    @Async
    @Transactional(propagation = TransactionDefinition.Propagation.REQUIRES_NEW)
    public void onApplicationEvent(MarketplaceItemHiddenEvent event) {
        log.info("Processing marketplace item hidden event: itemId={}, userId={}",
                event.getItemId(), event.getUserId());

        try {
            commentRepository.updateByParentTypeAndParentId("MARKETPLACE", event.getItemId(), true);
            log.debug("Hidden all comments for marketplace itemId={}", event.getItemId());
        } catch (Exception e) {
            log.error("Failed to hide comments for marketplace itemId={}: {}",
                    event.getItemId(), e.getMessage(), e);
        }

        log.info("Marketplace item hide propagation completed for itemId={}", event.getItemId());
    }
}
