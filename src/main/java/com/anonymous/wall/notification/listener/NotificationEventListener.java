package com.anonymous.wall.notification.listener;

import com.anonymous.wall.notification.device.DeviceTokenService;
import com.anonymous.wall.notification.event.CommentCreatedEvent;
import com.anonymous.wall.notification.service.PushNotificationService;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Singleton
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    @Inject
    private PushNotificationService pushNotificationService;

    @Inject
    private DeviceTokenService deviceTokenService;

    @EventListener
    @Async
    public void onCommentCreated(CommentCreatedEvent event) {
        if (event.getActorUserId().equals(event.getPostOwnerId())) {
            return;
        }

        List<String> tokens = deviceTokenService.getActiveTokens(event.getPostOwnerId());
        if (tokens.isEmpty()) {
            return;
        }

        Map<String, Object> data = Map.of(
                "type", "COMMENT",
                "postId", event.getPostId().toString()
        );

        for (String token : tokens) {
            pushNotificationService.sendPush(token, "New Comment", "Someone commented on your post", data);
        }

        log.debug("Push notifications sent for commentId={}, postId={}", event.getCommentId(), event.getPostId());
    }
}
