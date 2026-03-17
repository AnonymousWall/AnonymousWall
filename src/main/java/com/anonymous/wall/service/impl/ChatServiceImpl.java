package com.anonymous.wall.service.impl;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.entity.Conversation;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.ChatMessageDTO;
import com.anonymous.wall.model.ConversationDTO;
import com.anonymous.wall.notification.event.ChatMessageSentEvent;
import com.anonymous.wall.repository.ChatMessageRepository;
import com.anonymous.wall.repository.ConversationRepository;
import com.anonymous.wall.service.base.ChatService;
import com.anonymous.wall.service.base.UserBlockService;
import com.anonymous.wall.service.base.UserService;
import com.anonymous.wall.util.ConversationIdGenerator;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Implementation of ChatService.
 * Provides enterprise-grade chat functionality with proper validation, logging, and transaction management.
 * Uses conversationId for efficient querying and indexing.
 */
@Singleton
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    @Inject
    private ChatMessageRepository chatMessageRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private UserService userService;

    @Inject
    private UserBlockService userBlockService;

    @Inject
    private ApplicationEventPublisher<ChatMessageSentEvent> chatMessageEventPublisher;

    @Override
    @Transactional
    public ChatMessage sendMessage(UUID senderId, UUID receiverId, String content) {
        return sendMessage(senderId, receiverId, content, null);
    }

    @Override
    @Transactional
    public ChatMessage sendMessage(UUID senderId, UUID receiverId, String content, String imageUrl) {
        log.debug("Sending message from {} to {}", senderId, receiverId);

        if (senderId == null || receiverId == null) {
            throw new IllegalArgumentException("Sender ID and receiver ID must not be null");
        }

        boolean hasContent = content != null && !content.trim().isEmpty();
        boolean hasImage = imageUrl != null && !imageUrl.trim().isEmpty();
        if (!hasContent && !hasImage) {
            throw new IllegalArgumentException("Message must have content or an image");
        }

        if (hasContent && content.length() > 5000) {
            throw new IllegalArgumentException("Message content exceeds maximum length of 5000 characters");
        }

        Optional<UserEntity> senderOpt = userService.findById(senderId);
        if (senderOpt.isEmpty()) {
            throw new IllegalArgumentException("Sender not found");
        }

        Optional<UserEntity> receiverOpt = userService.findById(receiverId);
        if (receiverOpt.isEmpty()) {
            throw new IllegalArgumentException("Receiver not found");
        }

        UserEntity receiver = receiverOpt.get();
        UserEntity sender = senderOpt.get();

        if (receiver.isBlocked()) {
            throw new IllegalArgumentException("Cannot send message to a blocked user");
        }
        if (sender.isBlocked()) {
            throw new IllegalArgumentException("Blocked users cannot send messages");
        }
        if (userBlockService.getCombinedBlockedUserIds(senderId).contains(receiverId)) {
            throw new IllegalArgumentException("Cannot send message to this user");
        }

        UUID conversationId = ConversationIdGenerator.generate(senderId, receiverId);

        ChatMessage message = new ChatMessage(senderId, receiverId, conversationId,
                hasContent ? content.trim() : null);
        message.setImageUrl(imageUrl);
        message.setCreatedAt(OffsetDateTime.now());
        message.setReadStatus(false);

        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.info("Message sent from {} to {}, messageId={}, conversationId={}",
                senderId, receiverId, savedMessage.getId(), conversationId);

        // Update denormalized conversation rows for both participants
        upsertConversation(conversationId, senderId, receiverId,
                savedMessage, receiver.getProfileName(), false);
        upsertConversation(conversationId, receiverId, senderId,
                savedMessage, sender.getProfileName(), true);

        String preview = (savedMessage.getContent() != null && !savedMessage.getContent().isBlank())
                ? (savedMessage.getContent().length() > 50
                ? savedMessage.getContent().substring(0, 50) + "…"
                : savedMessage.getContent())
                : "\uD83D\uDCF7 Photo";

        chatMessageEventPublisher.publishEvent(new ChatMessageSentEvent(
                savedMessage.getId(),
                savedMessage.getConversationId(),
                senderId,
                receiverId,
                preview,
                sender.getProfileName()
        ));

        return savedMessage;
    }

    private void upsertConversation(UUID conversationId, UUID userId, UUID partnerId,
                                    ChatMessage msg, String partnerProfileName,
                                    boolean incrementUnread) {
        Optional<Conversation> existing = conversationRepository
                .findByConversationIdAndUserId(conversationId, userId);

        Conversation conv = existing.orElseGet(() -> {
            Conversation c = new Conversation();
            c.setConversationId(conversationId);
            c.setUserId(userId);
            c.setPartnerId(partnerId);
            c.setUnreadCount(0);
            return c;
        });

        conv.setPartnerProfileName(partnerProfileName);
        conv.setLastMessageContent(msg.getContent());
        conv.setLastMessageImageUrl(msg.getImageUrl());
        conv.setLastMessageSenderId(msg.getSenderId());
        conv.setLastMessageReceiverId(msg.getReceiverId());
        conv.setLastMessageReadStatus(msg.isReadStatus());
        conv.setLastMessageAt(msg.getCreatedAt());
        conv.setLastMessageId(msg.getId());

        if (incrementUnread) {
            conv.setUnreadCount(conv.getUnreadCount() + 1);
        }

        if (existing.isPresent()) {
            conversationRepository.update(conv);
        } else {
            conversationRepository.save(conv);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessage> getMessageHistory(UUID userId1, UUID userId2, Pageable pageable) {
        if (userId1 == null || userId2 == null) {
            throw new IllegalArgumentException("User IDs must not be null");
        }
        if (!userService.existsById(userId1)) {
            throw new IllegalArgumentException("User not found: " + userId1);
        }
        if (!userService.existsById(userId2)) {
            throw new IllegalArgumentException("User not found: " + userId2);
        }
        UUID conversationId = ConversationIdGenerator.generate(userId1, userId2);
        return chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDTO> getConversations(UUID userId) {
        log.debug("Getting conversations for user {}", userId);

        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        Set<UUID> blockedIds = userBlockService.getCombinedBlockedUserIds(userId);

        return conversationRepository.findByUserIdOrderByLastMessageAtDesc(userId)
                .stream()
                .filter(c -> !blockedIds.contains(c.getPartnerId()))
                .map(c -> toDTO(c, userId))
                .collect(java.util.stream.Collectors.toList());
    }

    private ConversationDTO toDTO(Conversation c, UUID userId) {
        ConversationDTO dto = new ConversationDTO();
        dto.setUserId(c.getPartnerId());
        dto.setProfileName(c.getPartnerProfileName());
        dto.setUnreadCount(c.getUnreadCount());

        ChatMessageDTO lastMsg = new ChatMessageDTO();
        lastMsg.setId(c.getLastMessageId());
        lastMsg.setSenderId(c.getLastMessageSenderId());
        lastMsg.setReceiverId(c.getLastMessageReceiverId());
        lastMsg.setContent(c.getLastMessageContent());
        lastMsg.setImageUrl(c.getLastMessageImageUrl());
        lastMsg.setReadStatus(c.isLastMessageReadStatus());
        lastMsg.setCreatedAt(c.getLastMessageAt());
        dto.setLastMessage(lastMsg);

        return dto;
    }

    @Override
    @Transactional
    public ChatMessage markMessageAsRead(UUID messageId, UUID userId) {
        if (messageId == null || userId == null) {
            throw new IllegalArgumentException("Message ID and user ID must not be null");
        }
        Optional<ChatMessage> messageOpt = chatMessageRepository.findById(messageId);
        if (messageOpt.isEmpty()) {
            throw new IllegalArgumentException("Message not found");
        }
        ChatMessage message = messageOpt.get();
        if (!message.getReceiverId().equals(userId)) {
            throw new IllegalArgumentException("Only the receiver can mark a message as read");
        }
        if (!message.isReadStatus()) {
            message.setReadStatus(true);
            chatMessageRepository.update(message);
        }
        return message;
    }

    @Override
    @Transactional
    public void markConversationAsRead(UUID receiverId, UUID senderId) {
        if (receiverId == null || senderId == null) {
            throw new IllegalArgumentException("Receiver ID and sender ID must not be null");
        }
        UUID conversationId = ConversationIdGenerator.generate(receiverId, senderId);
        chatMessageRepository.updateReadStatusByConversationIdAndReceiverId(
                conversationId, receiverId, true);

        // Reset unread count in conversation row
        conversationRepository.findByConversationIdAndUserId(conversationId, receiverId)
                .ifPresent(c -> {
                    c.setUnreadCount(0);
                    c.setLastMessageReadStatus(true);
                    conversationRepository.update(c);
                });

        log.info("Conversation {} marked as read for receiver {}", conversationId, receiverId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadMessages(UUID receiverId, UUID senderId) {
        if (receiverId == null || senderId == null) {
            throw new IllegalArgumentException("Receiver ID and sender ID must not be null");
        }
        UUID conversationId = ConversationIdGenerator.generate(receiverId, senderId);
        return chatMessageRepository.countByConversationIdAndReceiverIdAndReadStatusFalse(
                conversationId, receiverId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalUnreadMessages(UUID receiverId) {
        if (receiverId == null) {
            throw new IllegalArgumentException("Receiver ID must not be null");
        }
        return chatMessageRepository.countByReceiverIdAndReadStatusFalse(receiverId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getUnreadMessages(UUID receiverId, UUID senderId) {
        if (receiverId == null || senderId == null) {
            throw new IllegalArgumentException("Receiver ID and sender ID must not be null");
        }
        UUID conversationId = ConversationIdGenerator.generate(receiverId, senderId);
        return chatMessageRepository.findByConversationIdAndReceiverIdAndReadStatusFalse(
                conversationId, receiverId);
    }

//    @Override
//    @Transactional
//    public ChatMessage sendMessage(UUID senderId, UUID receiverId, String content) {
//        return sendMessage(senderId, receiverId, content, null);
//    }
//
//    @Override
//    @Transactional
//    public ChatMessage sendMessage(UUID senderId, UUID receiverId, String content, String imageUrl) {
//        log.debug("Sending message from {} to {}", senderId, receiverId);
//
//        // Validate input
//        if (senderId == null || receiverId == null) {
//            throw new IllegalArgumentException("Sender ID and receiver ID must not be null");
//        }
//
//        boolean hasContent = content != null && !content.trim().isEmpty();
//        boolean hasImage = imageUrl != null && !imageUrl.trim().isEmpty();
//        if (!hasContent && !hasImage) {
//            throw new IllegalArgumentException("Message must have content or an image");
//        }
//
//        if (hasContent && content.length() > 5000) {
//            throw new IllegalArgumentException("Message content exceeds maximum length of 5000 characters");
//        }
//
//        // Check if sender exists
//        Optional<UserEntity> senderOpt = userService.findById(senderId);
//        if (senderOpt.isEmpty()) {
//            log.warn("Sender not found: {}", senderId);
//            throw new IllegalArgumentException("Sender not found");
//        }
//
//        // Check if receiver exists
//        Optional<UserEntity> receiverOpt = userService.findById(receiverId);
//        if (receiverOpt.isEmpty()) {
//            log.warn("Receiver not found: {}", receiverId);
//            throw new IllegalArgumentException("Receiver not found");
//        }
//
//        UserEntity receiver = receiverOpt.get();
//
//        // Check if receiver is blocked (admin-level block)
//        if (receiver.isBlocked()) {
//            log.warn("Attempt to send message to blocked user: {}", receiverId);
//            throw new IllegalArgumentException("Cannot send message to a blocked user");
//        }
//
//        // Check if sender is blocked (shouldn't happen with proper auth, but safety check)
//        UserEntity sender = senderOpt.get();
//        if (sender.isBlocked()) {
//            log.warn("Blocked user attempting to send message: {}", senderId);
//            throw new IllegalArgumentException("Blocked users cannot send messages");
//        }
//
//        // Check user-level block in either direction using cached combined set
//        // getCombinedBlockedUserIds is cached — avoids two DB queries per message send
//        if (userBlockService.getCombinedBlockedUserIds(senderId).contains(receiverId)) {
//            log.warn("Cannot send message between users with a block relationship: {} and {}", senderId, receiverId);
//            throw new IllegalArgumentException("Cannot send message to this user");
//        }
//
//        // Generate deterministic conversation ID
//        UUID conversationId = ConversationIdGenerator.generate(senderId, receiverId);
//
//        // Create and save message
//        ChatMessage message = new ChatMessage(senderId, receiverId, conversationId,
//                hasContent ? content.trim() : null);
//        message.setImageUrl(imageUrl);
//        message.setCreatedAt(OffsetDateTime.now());
//        message.setReadStatus(false);
//
//        ChatMessage savedMessage = chatMessageRepository.save(message);
//        log.info("Message sent from {} to {}, message ID: {}, conversation ID: {}",
//                senderId, receiverId, savedMessage.getId(), conversationId);
//
//        String preview;
//        if (savedMessage.getContent() != null && !savedMessage.getContent().isBlank()) {
//            String messageContent = savedMessage.getContent();
//            preview = messageContent.length() > 50 ? messageContent.substring(0, 50) + "…" : messageContent;
//        } else {
//            preview = "\uD83D\uDCF7 Photo";
//        }
//
//        chatMessageEventPublisher.publishEvent(new ChatMessageSentEvent(
//                savedMessage.getId(),
//                savedMessage.getConversationId(),
//                senderId,
//                receiverId,
//                preview,
//                sender.getProfileName()
//        ));
//
//        return savedMessage;
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<ChatMessage> getMessageHistory(UUID userId1, UUID userId2, Pageable pageable) {
//        log.debug("Getting message history between {} and {}", userId1, userId2);
//
//        if (userId1 == null || userId2 == null) {
//            throw new IllegalArgumentException("User IDs must not be null");
//        }
//
//        // Validate users exist
//        if (!userService.existsById(userId1)) {
//            throw new IllegalArgumentException("User not found: " + userId1);
//        }
//        if (!userService.existsById(userId2)) {
//            throw new IllegalArgumentException("User not found: " + userId2);
//        }
//
//        // Generate conversation ID and query by it
//        UUID conversationId = ConversationIdGenerator.generate(userId1, userId2);
//        return chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<ConversationDTO> getConversations(UUID userId) {
//        log.debug("Getting conversations for user {}", userId);
//
//        if (userId == null) {
//            throw new IllegalArgumentException("User ID must not be null");
//        }
//
//        // Get list of conversation IDs for the user
//        List<UUID> conversationIds = chatMessageRepository.findDistinctConversationIdBySenderIdOrReceiverId(userId, userId);
//
//        // Build conversation DTOs
//        List<ConversationDTO> conversations = new ArrayList<>();
//        for (UUID conversationId : conversationIds) {
//            // Get last message
//            Optional<ChatMessage> lastMessageOpt = chatMessageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId);
//            if (lastMessageOpt.isEmpty()) {
//                log.warn("No last message found for conversation {}, skipping", conversationId);
//                continue;
//            }
//            ChatMessage lastMessage = lastMessageOpt.get();
//
//            // Get the other participant's ID
//            UUID partnerId = lastMessage.getSenderId().equals(userId)
//                    ? lastMessage.getReceiverId()
//                    : lastMessage.getSenderId();
//
//            // Get the other participant user info
//            Optional<UserEntity> partnerOpt = userService.findById(partnerId);
//            if (partnerOpt.isEmpty()) {
//                log.warn("Partner user not found: {}, skipping", partnerId);
//                continue;
//            }
//
//            UserEntity partner = partnerOpt.get();
//
//            // Skip conversations with users that have a block relationship
//            // Uses cached getCombinedBlockedUserIds — avoids DB hit per conversation
//            if (userBlockService.getCombinedBlockedUserIds(userId).contains(partnerId)) {
//                log.debug("Skipping conversation with blocked/blocking partner: {}", partnerId);
//                continue;
//            }
//
//            // Get unread count for this conversation
//            long unreadCount = chatMessageRepository.countByConversationIdAndReceiverIdAndReadStatusFalse(conversationId, userId);
//
//            // Build DTO
//            ConversationDTO conversation = new ConversationDTO();
//            conversation.setUserId(partnerId);
//            conversation.setProfileName(partner.getProfileName());
//            conversation.setUnreadCount((int) unreadCount);
//
//            ChatMessageDTO lastMessageDTO = new ChatMessageDTO();
//            lastMessageDTO.setId(lastMessage.getId());
//            lastMessageDTO.setSenderId(lastMessage.getSenderId());
//            lastMessageDTO.setReceiverId(lastMessage.getReceiverId());
//            lastMessageDTO.setContent(lastMessage.getContent());
//            lastMessageDTO.setImageUrl(lastMessage.getImageUrl());
//            lastMessageDTO.setReadStatus(lastMessage.isReadStatus());
//            lastMessageDTO.setCreatedAt(lastMessage.getCreatedAt());
//            conversation.setLastMessage(lastMessageDTO);
//
//            conversations.add(conversation);
//        }
//
//        // Sort by last message timestamp (most recent first)
//        conversations.sort((c1, c2) -> {
//            OffsetDateTime time1 = c1.getLastMessage() != null ? c1.getLastMessage().getCreatedAt() : OffsetDateTime.MIN;
//            OffsetDateTime time2 = c2.getLastMessage() != null ? c2.getLastMessage().getCreatedAt() : OffsetDateTime.MIN;
//            return time2.compareTo(time1);
//        });
//
//        log.debug("Found {} conversations for user {}", conversations.size(), userId);
//        return conversations;
//    }
//
//    @Override
//    @Transactional
//    public ChatMessage markMessageAsRead(UUID messageId, UUID userId) {
//        log.debug("Marking message {} as read by user {}", messageId, userId);
//
//        if (messageId == null || userId == null) {
//            throw new IllegalArgumentException("Message ID and user ID must not be null");
//        }
//
//        Optional<ChatMessage> messageOpt = chatMessageRepository.findById(messageId);
//        if (messageOpt.isEmpty()) {
//            throw new IllegalArgumentException("Message not found");
//        }
//
//        ChatMessage message = messageOpt.get();
//
//        // Verify user is the receiver
//        if (!message.getReceiverId().equals(userId)) {
//            log.warn("User {} attempted to mark message {} as read, but is not the receiver", userId, messageId);
//            throw new IllegalArgumentException("Only the receiver can mark a message as read");
//        }
//
//        // Mark as read if not already
//        if (!message.isReadStatus()) {
//            message.setReadStatus(true);
//            chatMessageRepository.update(message);
//            log.info("Message {} marked as read by user {}", messageId, userId);
//        }
//        return message;
//    }
//
//    @Override
//    @Transactional
//    public void markConversationAsRead(UUID receiverId, UUID senderId) {
//        log.debug("Marking all messages from {} to {} as read", senderId, receiverId);
//
//        if (receiverId == null || senderId == null) {
//            throw new IllegalArgumentException("Receiver ID and sender ID must not be null");
//        }
//
//        // Generate conversation ID
//        UUID conversationId = ConversationIdGenerator.generate(receiverId, senderId);
//
//        chatMessageRepository.updateReadStatusByConversationIdAndReceiverId(conversationId, receiverId, true);
//        log.info("All messages in conversation {} marked as read for receiver {}", conversationId, receiverId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public long countUnreadMessages(UUID receiverId, UUID senderId) {
//        if (receiverId == null || senderId == null) {
//            throw new IllegalArgumentException("Receiver ID and sender ID must not be null");
//        }
//
//        // Generate conversation ID
//        UUID conversationId = ConversationIdGenerator.generate(receiverId, senderId);
//
//        return chatMessageRepository.countByConversationIdAndReceiverIdAndReadStatusFalse(conversationId, receiverId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public long countTotalUnreadMessages(UUID receiverId) {
//        if (receiverId == null) {
//            throw new IllegalArgumentException("Receiver ID must not be null");
//        }
//
//        return chatMessageRepository.countByReceiverIdAndReadStatusFalse(receiverId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<ChatMessage> getUnreadMessages(UUID receiverId, UUID senderId) {
//        if (receiverId == null || senderId == null) {
//            throw new IllegalArgumentException("Receiver ID and sender ID must not be null");
//        }
//
//        UUID conversationId = ConversationIdGenerator.generate(receiverId, senderId);
//        return chatMessageRepository.findByConversationIdAndReceiverIdAndReadStatusFalse(conversationId, receiverId);
//    }
}