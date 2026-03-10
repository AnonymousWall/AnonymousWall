package com.anonymous.wall.listener;

import com.anonymous.wall.event.ProfileNameChangedEvent;
import com.anonymous.wall.service.base.CommentsService;
import com.anonymous.wall.service.base.InternshipService;
import com.anonymous.wall.service.base.MarketplaceService;
import com.anonymous.wall.service.base.PostsService;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.retry.annotation.Retryable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ProfileNameUpdateEventListener implements ApplicationEventListener<ProfileNameChangedEvent> {
    
    private static final Logger log = LoggerFactory.getLogger(ProfileNameUpdateEventListener.class);

    @Inject
    private PostsService postsService;

    @Inject
    private CommentsService commentsService;

    @Inject
    private InternshipService internshipService;

    @Inject
    private MarketplaceService marketplaceService;

    @Override
    @Async(TaskExecutors.IO)
    @Retryable(
            attempts = "3",
            delay = "50ms",
            multiplier = "2.0"
    )
    public void onApplicationEvent(ProfileNameChangedEvent event) {
        log.info("Processing profile name change: userId={}, newName={}",
                event.getUserId(), event.getNewName());
        postsService.updateProfileNameByUserId(event.getUserId(), event.getNewName());
        commentsService.updateProfileNameByUserId(event.getUserId(), event.getNewName());
        internshipService.updateProfileNameByUserId(event.getUserId(), event.getNewName());
        marketplaceService.updateProfileNameByUserId(event.getUserId(), event.getNewName());
        log.info("Profile name propagation completed for userId={}", event.getUserId());
    }
}
