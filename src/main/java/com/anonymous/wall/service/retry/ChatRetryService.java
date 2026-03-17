package com.anonymous.wall.service.retry;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.model.ConversationDTO;
import com.anonymous.wall.service.base.ChatService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.UUID;

/**
 * Chat retry wrapper.
 */
@Singleton
public class ChatRetryService {

    private final ChatService chatService;

    public ChatRetryService(ChatService chatService) {
        this.chatService = chatService;
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public ChatMessage sendMessage(UUID senderId, UUID receiverId, String content) {
        return chatService.sendMessage(senderId, receiverId, content);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public ChatMessage sendMessage(UUID senderId, UUID receiverId, String content, String imageUrl) {
        return chatService.sendMessage(senderId, receiverId, content, imageUrl);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Page<ChatMessage> getMessageHistory(UUID userId1, UUID userId2, Pageable pageable) {
        return chatService.getMessageHistory(userId1, userId2, pageable);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public List<ConversationDTO> getConversations(UUID userId) {
        return chatService.getConversations(userId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public ChatMessage markMessageAsRead(UUID messageId, UUID userId) {
        return chatService.markMessageAsRead(messageId, userId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public void markConversationAsRead(UUID receiverId, UUID senderId) {
        chatService.markConversationAsRead(receiverId, senderId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public long countUnreadMessages(UUID receiverId, UUID senderId) {
        return chatService.countUnreadMessages(receiverId, senderId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public long countTotalUnreadMessages(UUID userId) {
        return chatService.countTotalUnreadMessages(userId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public List<ChatMessage> getUnreadMessages(UUID receiverId, UUID senderId) {
        return chatService.getUnreadMessages(receiverId, senderId);
    }
}