package com.anonymous.wall.service;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.ChatMessageDTO;
import com.anonymous.wall.model.ConversationDTO;
import com.anonymous.wall.repository.ChatMessageRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ChatService.
 * Provides enterprise-grade chat functionality with proper validation, logging, and transaction management.
 */
@Singleton
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    @Inject
    private ChatMessageRepository chatMessageRepository;

    @Inject
    private UserRepository userRepository;

    @Override
    @Transactional
    @Retryable
    public ChatMessage sendMessage(UUID senderId, UUID receiverId, String content) {
        log.debug("Sending message from {} to {}", senderId, receiverId);

        // Validate input
        if (senderId == null || receiverId == null) {
            throw new IllegalArgumentException("Sender ID and receiver ID must not be null");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content must not be empty");
        }

        if (content.length() > 5000) {
            throw new IllegalArgumentException("Message content exceeds maximum length of 5000 characters");
        }

        // Check if sender exists
        Optional<UserEntity> senderOpt = userRepository.findById(senderId);
        if (senderOpt.isEmpty()) {
            log.warn("Sender not found: {}", senderId);
            throw new IllegalArgumentException("Sender not found");
        }

        // Check if receiver exists
        Optional<UserEntity> receiverOpt = userRepository.findById(receiverId);
        if (receiverOpt.isEmpty()) {
            log.warn("Receiver not found: {}", receiverId);
            throw new IllegalArgumentException("Receiver not found");
        }

        UserEntity receiver = receiverOpt.get();

        // Check if receiver is blocked
        if (receiver.isBlocked()) {
            log.warn("Attempt to send message to blocked user: {}", receiverId);
            throw new IllegalArgumentException("Cannot send message to a blocked user");
        }

        // Check if sender is blocked (shouldn't happen with proper auth, but safety check)
        UserEntity sender = senderOpt.get();
        if (sender.isBlocked()) {
            log.warn("Blocked user attempting to send message: {}", senderId);
            throw new IllegalArgumentException("Blocked users cannot send messages");
        }

        // Create and save message
        ChatMessage message = new ChatMessage(senderId, receiverId, content.trim());
        message.setCreatedAt(OffsetDateTime.now());
        message.setReadStatus(false);

        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.info("Message sent from {} to {}, message ID: {}", senderId, receiverId, savedMessage.getId());

        return savedMessage;
    }

    @Override
    public Page<ChatMessage> getMessageHistory(UUID userId1, UUID userId2, Pageable pageable) {
        log.debug("Getting message history between {} and {}", userId1, userId2);

        if (userId1 == null || userId2 == null) {
            throw new IllegalArgumentException("User IDs must not be null");
        }

        // Validate users exist
        if (!userRepository.existsById(userId1)) {
            throw new IllegalArgumentException("User not found: " + userId1);
        }
        if (!userRepository.existsById(userId2)) {
            throw new IllegalArgumentException("User not found: " + userId2);
        }

        return chatMessageRepository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(
                userId1, userId2, userId2, userId1, pageable);
    }

    @Override
    public List<ConversationDTO> getConversations(UUID userId) {
        log.debug("Getting conversations for user {}", userId);

        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        // Get list of users with conversations
        List<UUID> partnerIds = chatMessageRepository.findConversationPartners(userId);

        // Build conversation DTOs
        List<ConversationDTO> conversations = new ArrayList<>();
        for (UUID partnerId : partnerIds) {
            // Get partner user info
            Optional<UserEntity> partnerOpt = userRepository.findById(partnerId);
            if (partnerOpt.isEmpty()) {
                log.warn("Partner user not found: {}, skipping", partnerId);
                continue;
            }

            UserEntity partner = partnerOpt.get();

            // Get last message
            ChatMessage lastMessage = chatMessageRepository.findLastMessageBetweenUsers(userId, partnerId);

            // Get unread count
            long unreadCount = chatMessageRepository.countByReceiverIdAndSenderIdAndReadStatusFalse(userId, partnerId);

            // Build DTO
            ConversationDTO conversation = new ConversationDTO();
            conversation.setUserId(partnerId);
            conversation.setProfileName(partner.getProfileName());
            conversation.setUnreadCount((int) unreadCount);

            if (lastMessage != null) {
                ChatMessageDTO lastMessageDTO = new ChatMessageDTO();
                lastMessageDTO.setId(lastMessage.getId());
                lastMessageDTO.setSenderId(lastMessage.getSenderId());
                lastMessageDTO.setReceiverId(lastMessage.getReceiverId());
                lastMessageDTO.setContent(lastMessage.getContent());
                lastMessageDTO.setReadStatus(lastMessage.isReadStatus());
                lastMessageDTO.setCreatedAt(lastMessage.getCreatedAt());
                conversation.setLastMessage(lastMessageDTO);
            }

            conversations.add(conversation);
        }

        // Sort by last message timestamp (most recent first)
        conversations.sort((c1, c2) -> {
            OffsetDateTime time1 = c1.getLastMessage() != null ? c1.getLastMessage().getCreatedAt() : OffsetDateTime.MIN;
            OffsetDateTime time2 = c2.getLastMessage() != null ? c2.getLastMessage().getCreatedAt() : OffsetDateTime.MIN;
            return time2.compareTo(time1);
        });

        log.debug("Found {} conversations for user {}", conversations.size(), userId);
        return conversations;
    }

    @Override
    @Transactional
    public void markMessageAsRead(UUID messageId, UUID userId) {
        log.debug("Marking message {} as read by user {}", messageId, userId);

        if (messageId == null || userId == null) {
            throw new IllegalArgumentException("Message ID and user ID must not be null");
        }

        Optional<ChatMessage> messageOpt = chatMessageRepository.findById(messageId);
        if (messageOpt.isEmpty()) {
            throw new IllegalArgumentException("Message not found");
        }

        ChatMessage message = messageOpt.get();

        // Verify user is the receiver
        if (!message.getReceiverId().equals(userId)) {
            log.warn("User {} attempted to mark message {} as read, but is not the receiver", userId, messageId);
            throw new IllegalArgumentException("Only the receiver can mark a message as read");
        }

        // Mark as read if not already
        if (!message.isReadStatus()) {
            message.setReadStatus(true);
            chatMessageRepository.update(message);
            log.info("Message {} marked as read by user {}", messageId, userId);
        }
    }

    @Override
    @Transactional
    public void markConversationAsRead(UUID receiverId, UUID senderId) {
        log.debug("Marking all messages from {} to {} as read", senderId, receiverId);

        if (receiverId == null || senderId == null) {
            throw new IllegalArgumentException("Receiver ID and sender ID must not be null");
        }

        chatMessageRepository.markMessagesAsRead(receiverId, senderId);
        log.info("All messages from {} to {} marked as read", senderId, receiverId);
    }

    @Override
    public long countUnreadMessages(UUID receiverId, UUID senderId) {
        if (receiverId == null || senderId == null) {
            throw new IllegalArgumentException("Receiver ID and sender ID must not be null");
        }

        return chatMessageRepository.countByReceiverIdAndSenderIdAndReadStatusFalse(receiverId, senderId);
    }

    @Override
    public long countTotalUnreadMessages(UUID receiverId) {
        if (receiverId == null) {
            throw new IllegalArgumentException("Receiver ID must not be null");
        }

        return chatMessageRepository.countByReceiverIdAndReadStatusFalse(receiverId);
    }
}
