package com.anonymous.wall.notification.listener;

import com.anonymous.wall.controller.ChatWebSocketHandler;
import com.anonymous.wall.notification.device.DeviceTokenService;
import com.anonymous.wall.notification.event.ChatMessageSentEvent;
import com.anonymous.wall.notification.event.CommentCreatedEvent;
import com.anonymous.wall.notification.event.InternshipCommentCreatedEvent;
import com.anonymous.wall.notification.event.MarketplaceCommentCreatedEvent;
import com.anonymous.wall.service.base.NotificationService;
import com.anonymous.wall.notification.service.PushNotificationService;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.Async;
import io.micronaut.transaction.TransactionDefinition;
import io.micronaut.transaction.annotation.Transactional;
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

    @Inject
    private ChatWebSocketHandler chatWebSocketHandler;

    @Inject
    private NotificationService notificationService;

    @EventListener
    @Async(TaskExecutors.IO)
    @Transactional(propagation = TransactionDefinition.Propagation.REQUIRES_NEW)
    public void onCommentCreated(CommentCreatedEvent event) {
        if (event.getActorUserId().equals(event.getPostOwnerId())) {
            return;
        }

        notificationService.createNotification(
                event.getPostOwnerId(),
                event.getActorUserId(),
                "COMMENT",
                event.getPostId(),
                null,
                null
        );

        List<String> tokens = deviceTokenService.getActiveTokens(event.getPostOwnerId());
        if (tokens.isEmpty()) {
            return;
        }

        Map<String, Object> data = Map.of(
                "type", "COMMENT",
                "postId", event.getPostId().toString(),
                "wall", event.getWall()
        );

        for (String token : tokens) {
            pushNotificationService.sendPush(token, "New Comment", "Someone commented on your post", data);
        }

        log.debug("Push notifications sent for commentId={}, postId={}", event.getCommentId(), event.getPostId());
    }

    @EventListener
    @Async(TaskExecutors.IO)
    @Transactional(propagation = TransactionDefinition.Propagation.REQUIRES_NEW)
    public void onInternshipCommentCreated(InternshipCommentCreatedEvent event) {
        if (event.getActorUserId().equals(event.getInternshipOwnerId())) return;

        notificationService.createNotification(
                event.getInternshipOwnerId(),
                event.getActorUserId(),
                "INTERNSHIP_COMMENT",
                event.getInternshipId(),
                null,
                null
        );

        List<String> tokens = deviceTokenService.getActiveTokens(event.getInternshipOwnerId());
        if (tokens.isEmpty()) return;

        Map<String, Object> data = Map.of(
                "type", "INTERNSHIP_COMMENT",
                "internshipId", event.getInternshipId().toString()
        );

        for (String token : tokens) {
            pushNotificationService.sendPush(token, "New Comment", "Someone commented on your internship posting", data);
        }

        log.debug("Push notifications sent for internship commentId={}, internshipId={}",
                event.getCommentId(), event.getInternshipId());
    }

    @EventListener
    @Async(TaskExecutors.IO)
    @Transactional(propagation = TransactionDefinition.Propagation.REQUIRES_NEW)
    public void onMarketplaceCommentCreated(MarketplaceCommentCreatedEvent event) {
        if (event.getActorUserId().equals(event.getItemOwnerId())) return;

        notificationService.createNotification(
                event.getItemOwnerId(),
                event.getActorUserId(),
                "MARKETPLACE_COMMENT",
                event.getItemId(),
                null,
                null
        );

        List<String> tokens = deviceTokenService.getActiveTokens(event.getItemOwnerId());
        if (tokens.isEmpty()) return;

        Map<String, Object> data = Map.of(
                "type", "MARKETPLACE_COMMENT",
                "itemId", event.getItemId().toString()
        );

        for (String token : tokens) {
            pushNotificationService.sendPush(token, "New Comment", "Someone commented on your marketplace listing", data);
        }

        log.debug("Push notifications sent for marketplace commentId={}, itemId={}",
                event.getCommentId(), event.getItemId());
    }

    @EventListener
    @Async(TaskExecutors.IO)
    @Transactional(propagation = TransactionDefinition.Propagation.REQUIRES_NEW)
    public void onChatMessageSent(ChatMessageSentEvent event) {
        if (event.getSenderUserId().equals(event.getRecipientUserId())) return;

        if (chatWebSocketHandler.isUserConnected(event.getRecipientUserId())) {
            log.debug("Skipping push — recipient {} is connected via WebSocket",
                    event.getRecipientUserId());
            return;
        }

        List<String> tokens = deviceTokenService.getActiveTokens(event.getRecipientUserId());
        if (tokens.isEmpty()) return;

        Map<String, Object> data = Map.of(
                "type", "CHAT_MESSAGE",
                "conversationId", event.getConversationId().toString(),
                "senderUserId", event.getSenderUserId().toString()
        );

        for (String token : tokens) {
            pushNotificationService.sendPush(token, event.getSenderProfileName(),
                    event.getMessagePreview(), data);
        }

        log.debug("Push notifications sent for chat messageId={}, conversationId={}",
                event.getMessageId(), event.getConversationId());
    }
}
