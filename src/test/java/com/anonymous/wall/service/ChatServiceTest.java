package com.anonymous.wall.service;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.entity.Conversation;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.ConversationDTO;
import com.anonymous.wall.notification.event.ChatMessageSentEvent;
import com.anonymous.wall.repository.ChatMessageRepository;
import com.anonymous.wall.repository.ConversationRepository;
import com.anonymous.wall.service.base.UserBlockService;
import com.anonymous.wall.service.base.UserService;
import com.anonymous.wall.service.impl.ChatServiceImpl;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("ChatService Unit Tests")
class ChatServiceTest {

    private ChatServiceImpl chatService;
    private ChatMessageRepository chatMessageRepository;
    private ConversationRepository conversationRepository;
    private UserService userService;
    private UserBlockService userBlockService;
    @SuppressWarnings("unchecked")
    private ApplicationEventPublisher<ChatMessageSentEvent> chatMessageEventPublisher;

    private UUID testUser1Id;
    private UUID testUser2Id;
    private UUID blockedUserId;
    private UserEntity testUser1;
    private UserEntity testUser2;
    private UserEntity blockedUser;

    @BeforeEach
    void setUp() {
        chatMessageRepository = mock(ChatMessageRepository.class);
        conversationRepository = mock(ConversationRepository.class);
        userService = mock(UserService.class);
        userBlockService = mock(UserBlockService.class);
        chatMessageEventPublisher = mock(ApplicationEventPublisher.class);

        chatService = new ChatServiceImpl();

        try {
            setField("chatMessageRepository", chatMessageRepository);
            setField("conversationRepository", conversationRepository);
            setField("userService", userService);
            setField("userBlockService", userBlockService);
            setField("chatMessageEventPublisher", chatMessageEventPublisher);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        testUser1Id = UUID.randomUUID();
        testUser2Id = UUID.randomUUID();
        blockedUserId = UUID.randomUUID();

        testUser1 = new UserEntity();
        testUser1.setId(testUser1Id);
        testUser1.setEmail("user1@harvard.edu");
        testUser1.setSchoolDomain("harvard.edu");
        testUser1.setProfileName("User1");
        testUser1.setBlocked(false);

        testUser2 = new UserEntity();
        testUser2.setId(testUser2Id);
        testUser2.setEmail("user2@harvard.edu");
        testUser2.setSchoolDomain("harvard.edu");
        testUser2.setProfileName("User2");
        testUser2.setBlocked(false);

        blockedUser = new UserEntity();
        blockedUser.setId(blockedUserId);
        blockedUser.setEmail("blocked@harvard.edu");
        blockedUser.setSchoolDomain("harvard.edu");
        blockedUser.setProfileName("BlockedUser");
        blockedUser.setBlocked(true);

        when(userBlockService.getCombinedBlockedUserIds(any(UUID.class)))
                .thenReturn(Collections.emptySet());

        // Default: no existing conversation rows
        when(conversationRepository.findByConversationIdAndUserId(any(), any()))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(conversationRepository.update(any(Conversation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private void setField(String name, Object value) throws Exception {
        var field = ChatServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(chatService, value);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private void stubValidSend() {
        when(userService.findById(testUser1Id)).thenReturn(Optional.of(testUser1));
        when(userService.findById(testUser2Id)).thenReturn(Optional.of(testUser2));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage m = invocation.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
    }

    private Conversation conversation(UUID conversationId, UUID userId, UUID partnerId,
                                      String partnerProfileName, int unreadCount) {
        Conversation c = new Conversation();
        c.setConversationId(conversationId);
        c.setUserId(userId);
        c.setPartnerId(partnerId);
        c.setPartnerProfileName(partnerProfileName);
        c.setLastMessageContent("Last message");
        c.setLastMessageSenderId(partnerId);
        c.setLastMessageReceiverId(userId);
        c.setLastMessageReadStatus(false);
        c.setLastMessageAt(OffsetDateTime.now());
        c.setUnreadCount(unreadCount);
        return c;
    }

    // ─── Send Message — Positive ───────────────────────────────────────────────

    @Nested
    @DisplayName("Send Message — Positive Cases")
    class SendMessagePositiveCases {

        @Test
        @DisplayName("Should send text message successfully")
        void shouldSendMessageSuccessfully() {
            stubValidSend();

            ChatMessage result = chatService.sendMessage(testUser1Id, testUser2Id, "Hello, this is a test message!");

            assertNotNull(result);
            assertEquals("Hello, this is a test message!", result.getContent());
            assertEquals(testUser1Id, result.getSenderId());
            assertEquals(testUser2Id, result.getReceiverId());
            assertNotNull(result.getConversationId());
            assertFalse(result.isReadStatus());
            assertNotNull(result.getCreatedAt());
            verify(chatMessageRepository).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should upsert conversation rows for both participants on send")
        void shouldUpsertConversationRowsForBothParticipants() {
            stubValidSend();

            chatService.sendMessage(testUser1Id, testUser2Id, "Hello!");

            // Two upserts — one per participant
            verify(conversationRepository, times(2)).save(any(Conversation.class));
        }

        @Test
        @DisplayName("Should update existing conversation row instead of insert when row exists")
        void shouldUpdateExistingConversationRow() {
            stubValidSend();
            UUID convId = com.anonymous.wall.util.ConversationIdGenerator.generate(testUser1Id, testUser2Id);
            Conversation existing = conversation(convId, testUser1Id, testUser2Id, "User2", 0);
            when(conversationRepository.findByConversationIdAndUserId(eq(convId), eq(testUser1Id)))
                    .thenReturn(Optional.of(existing));

            chatService.sendMessage(testUser1Id, testUser2Id, "Hello!");

            verify(conversationRepository, atLeastOnce()).update(any(Conversation.class));
        }

        @Test
        @DisplayName("Should increment unread count on receiver's conversation row")
        void shouldIncrementUnreadCountForReceiver() {
            stubValidSend();
            UUID convId = com.anonymous.wall.util.ConversationIdGenerator.generate(testUser1Id, testUser2Id);
            Conversation receiverConv = conversation(convId, testUser2Id, testUser1Id, "User1", 2);
            when(conversationRepository.findByConversationIdAndUserId(eq(convId), eq(testUser2Id)))
                    .thenReturn(Optional.of(receiverConv));

            chatService.sendMessage(testUser1Id, testUser2Id, "Hello!");

            ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
            verify(conversationRepository, atLeastOnce()).update(captor.capture());

            Conversation updated = captor.getAllValues().stream()
                    .filter(c -> c.getUserId().equals(testUser2Id))
                    .findFirst().orElseThrow();
            assertEquals(3, updated.getUnreadCount(), "Receiver unread count must be incremented");
        }

        @Test
        @DisplayName("Should trim whitespace from message content")
        void shouldTrimMessageContent() {
            stubValidSend();
            ChatMessage result = chatService.sendMessage(testUser1Id, testUser2Id, "  Hello with spaces  ");
            assertEquals("Hello with spaces", result.getContent());
        }

        @Test
        @DisplayName("Should send image-only message")
        void shouldSendImageOnlyMessage() {
            stubValidSend();
            ChatMessage result = chatService.sendMessage(testUser1Id, testUser2Id, null,
                    "https://example.com/chat/image.jpg");
            assertNotNull(result);
            assertNull(result.getContent());
            assertEquals("https://example.com/chat/image.jpg", result.getImageUrl());
        }

        @Test
        @DisplayName("Should accept content of exactly 5000 characters — boundary")
        void shouldAcceptContentAtExactLimit() {
            stubValidSend();
            assertDoesNotThrow(() ->
                    chatService.sendMessage(testUser1Id, testUser2Id, "a".repeat(5000)));
        }

        @Test
        @DisplayName("Should generate deterministic conversation ID")
        void shouldGenerateDeterministicConversationId() {
            stubValidSend();
            ChatMessage msg1 = chatService.sendMessage(testUser1Id, testUser2Id, "A");

            when(userService.findById(testUser2Id)).thenReturn(Optional.of(testUser2));
            when(userService.findById(testUser1Id)).thenReturn(Optional.of(testUser1));
            ChatMessage msg2 = chatService.sendMessage(testUser2Id, testUser1Id, "B");

            assertEquals(msg1.getConversationId(), msg2.getConversationId());
        }

        @Test
        @DisplayName("Should publish ChatMessageSentEvent after saving message")
        void shouldPublishEventAfterSaving() {
            stubValidSend();
            chatService.sendMessage(testUser1Id, testUser2Id, "Hello!");

            ArgumentCaptor<ChatMessageSentEvent> captor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
            verify(chatMessageEventPublisher).publishEvent(captor.capture());

            ChatMessageSentEvent event = captor.getValue();
            assertEquals(testUser1Id, event.getSenderUserId());
            assertEquals(testUser2Id, event.getRecipientUserId());
        }

        @Test
        @DisplayName("Should truncate preview for long content")
        void shouldTruncatePreviewForLongContent() {
            stubValidSend();
            chatService.sendMessage(testUser1Id, testUser2Id, "a".repeat(100));

            ArgumentCaptor<ChatMessageSentEvent> captor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
            verify(chatMessageEventPublisher).publishEvent(captor.capture());

            String preview = captor.getValue().getMessagePreview();
            assertEquals(51, preview.length());
            assertTrue(preview.endsWith("…"));
        }

        @Test
        @DisplayName("Should set photo preview for image-only message")
        void shouldSetPhotoPreviewForImageOnlyMessage() {
            stubValidSend();
            chatService.sendMessage(testUser1Id, testUser2Id, null, "https://example.com/image.jpg");

            ArgumentCaptor<ChatMessageSentEvent> captor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
            verify(chatMessageEventPublisher).publishEvent(captor.capture());

            assertEquals("\uD83D\uDCF7 Photo", captor.getValue().getMessagePreview());
        }

        @Test
        @DisplayName("Should include sender profile name in event")
        void shouldIncludeSenderProfileNameInEvent() {
            stubValidSend();
            chatService.sendMessage(testUser1Id, testUser2Id, "Hello!");

            ArgumentCaptor<ChatMessageSentEvent> captor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
            verify(chatMessageEventPublisher).publishEvent(captor.capture());

            assertEquals("User1", captor.getValue().getSenderProfileName());
        }
    }

    // ─── Send Message — Validation ─────────────────────────────────────────────

    @Nested
    @DisplayName("Send Message — Validation Errors")
    class SendMessageValidationErrors {

        @Test
        @DisplayName("Should throw when sender ID is null")
        void shouldThrowWhenSenderIdNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(null, testUser2Id, "Test message"));
            assertEquals("Sender ID and receiver ID must not be null", ex.getMessage());
            verifyNoInteractions(chatMessageRepository);
        }

        @Test
        @DisplayName("Should throw when receiver ID is null")
        void shouldThrowWhenReceiverIdNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(testUser1Id, null, "Test message"));
            assertEquals("Sender ID and receiver ID must not be null", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when both content and imageUrl are null")
        void shouldThrowWhenBothNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(testUser1Id, testUser2Id, null, null));
            assertEquals("Message must have content or an image", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when content exceeds 5000 characters")
        void shouldThrowWhenContentTooLong() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(testUser1Id, testUser2Id, "a".repeat(5001)));
            assertEquals("Message content exceeds maximum length of 5000 characters", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when sender not found")
        void shouldThrowWhenSenderNotFound() {
            when(userService.findById(testUser1Id)).thenReturn(Optional.empty());
            assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(testUser1Id, testUser2Id, "Test"));
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when receiver not found")
        void shouldThrowWhenReceiverNotFound() {
            when(userService.findById(testUser1Id)).thenReturn(Optional.of(testUser1));
            when(userService.findById(testUser2Id)).thenReturn(Optional.empty());
            assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(testUser1Id, testUser2Id, "Test"));
            verify(chatMessageRepository, never()).save(any());
        }
    }

    // ─── Send Message — Block Checks ───────────────────────────────────────────

    @Nested
    @DisplayName("Send Message — Block Checks")
    class SendMessageBlockChecks {

        @Test
        @DisplayName("Should throw when receiver is admin-blocked")
        void shouldThrowWhenReceiverAdminBlocked() {
            when(userService.findById(testUser1Id)).thenReturn(Optional.of(testUser1));
            when(userService.findById(blockedUserId)).thenReturn(Optional.of(blockedUser));
            assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(testUser1Id, blockedUserId, "Test"));
        }

        @Test
        @DisplayName("Should throw when user-level block exists")
        void shouldThrowWhenUserLevelBlockExists() {
            when(userService.findById(testUser1Id)).thenReturn(Optional.of(testUser1));
            when(userService.findById(testUser2Id)).thenReturn(Optional.of(testUser2));
            when(userBlockService.getCombinedBlockedUserIds(testUser1Id))
                    .thenReturn(Set.of(testUser2Id));
            assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(testUser1Id, testUser2Id, "Test"));
        }
    }

    // ─── Get Message History ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Get Message History")
    class GetMessageHistory {

        @Test
        @DisplayName("Should return paginated history for valid users")
        void shouldReturnHistoryForValidUsers() {
            Pageable pageable = Pageable.from(0, 50);
            Page<ChatMessage> mockPage = mock(Page.class);
            when(userService.existsById(testUser1Id)).thenReturn(true);
            when(userService.existsById(testUser2Id)).thenReturn(true);
            when(chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(any(), eq(pageable)))
                    .thenReturn(mockPage);

            Page<ChatMessage> result = chatService.getMessageHistory(testUser1Id, testUser2Id, pageable);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw when user IDs are null")
        void shouldThrowWhenUserIdsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> chatService.getMessageHistory(null, testUser2Id, Pageable.from(0, 50)));
            assertThrows(IllegalArgumentException.class,
                    () -> chatService.getMessageHistory(testUser1Id, null, Pageable.from(0, 50)));
        }

        @Test
        @DisplayName("Should throw when users not found")
        void shouldThrowWhenUsersNotFound() {
            when(userService.existsById(testUser1Id)).thenReturn(false);
            assertThrows(IllegalArgumentException.class,
                    () -> chatService.getMessageHistory(testUser1Id, testUser2Id, Pageable.from(0, 50)));
        }
    }

    // ─── Mark Message As Read ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Mark Message As Read")
    class MarkMessageAsRead {

        @Test
        @DisplayName("Should mark unread message as read")
        void shouldMarkUnreadMessageAsRead() {
            UUID messageId = UUID.randomUUID();
            ChatMessage message = new ChatMessage(testUser1Id, testUser2Id, "Test");
            message.setId(messageId);
            message.setReadStatus(false);
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));
            when(chatMessageRepository.update(any())).thenReturn(message);

            chatService.markMessageAsRead(messageId, testUser2Id);

            assertTrue(message.isReadStatus());
            verify(chatMessageRepository).update(message);
        }

        @Test
        @DisplayName("Should not call update when already read")
        void shouldNotUpdateIfAlreadyRead() {
            UUID messageId = UUID.randomUUID();
            ChatMessage message = new ChatMessage(testUser1Id, testUser2Id, "Test");
            message.setId(messageId);
            message.setReadStatus(true);
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));

            chatService.markMessageAsRead(messageId, testUser2Id);

            verify(chatMessageRepository, never()).update(any());
        }

        @Test
        @DisplayName("Should throw when caller is sender not receiver")
        void shouldThrowWhenCallerIsNotReceiver() {
            UUID messageId = UUID.randomUUID();
            ChatMessage message = new ChatMessage(testUser1Id, testUser2Id, "Test");
            message.setId(messageId);
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.markMessageAsRead(messageId, testUser1Id));
            assertEquals("Only the receiver can mark a message as read", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when message not found")
        void shouldThrowWhenMessageNotFound() {
            UUID messageId = UUID.randomUUID();
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.empty());
            assertThrows(IllegalArgumentException.class,
                    () -> chatService.markMessageAsRead(messageId, testUser2Id));
        }
    }

    // ─── Mark Conversation As Read ─────────────────────────────────────────────

    @Nested
    @DisplayName("Mark Conversation As Read")
    class MarkConversationAsRead {

        @Test
        @DisplayName("Should bulk-mark all messages as read and reset unread count")
        void shouldMarkAllMessagesAsReadAndResetUnread() {
            UUID convId = com.anonymous.wall.util.ConversationIdGenerator.generate(testUser2Id, testUser1Id);
            Conversation conv = conversation(convId, testUser2Id, testUser1Id, "User1", 5);
            when(conversationRepository.findByConversationIdAndUserId(eq(convId), eq(testUser2Id)))
                    .thenReturn(Optional.of(conv));

            chatService.markConversationAsRead(testUser2Id, testUser1Id);

            verify(chatMessageRepository).updateReadStatusByConversationIdAndReceiverId(
                    any(), eq(testUser2Id), eq(true));

            ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
            verify(conversationRepository).update(captor.capture());
            assertEquals(0, captor.getValue().getUnreadCount());
        }

        @Test
        @DisplayName("Should throw when IDs are null")
        void shouldThrowWhenIdsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> chatService.markConversationAsRead(null, testUser1Id));
            assertThrows(IllegalArgumentException.class,
                    () -> chatService.markConversationAsRead(testUser2Id, null));
        }
    }

    // ─── Count Unread Messages ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Count Unread Messages")
    class CountUnreadMessages {

        @Test
        @DisplayName("Should count unread messages from specific sender")
        void shouldCountUnreadMessages() {
            when(chatMessageRepository.countByConversationIdAndReceiverIdAndReadStatusFalse(
                    any(), eq(testUser2Id))).thenReturn(5L);
            assertEquals(5L, chatService.countUnreadMessages(testUser2Id, testUser1Id));
        }

        @Test
        @DisplayName("Should count total unread messages")
        void shouldCountTotalUnread() {
            when(chatMessageRepository.countByReceiverIdAndReadStatusFalse(testUser2Id)).thenReturn(10L);
            assertEquals(10L, chatService.countTotalUnreadMessages(testUser2Id));
        }

        @Test
        @DisplayName("Should throw when IDs are null")
        void shouldThrowWhenIdsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> chatService.countUnreadMessages(null, testUser1Id));
            assertThrows(IllegalArgumentException.class,
                    () -> chatService.countTotalUnreadMessages(null));
        }
    }

    // ─── Get Unread Messages ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Get Unread Messages")
    class GetUnreadMessages {

        @Test
        @DisplayName("Should return unread messages")
        void shouldReturnUnreadMessages() {
            ChatMessage msg = new ChatMessage(testUser1Id, testUser2Id, "Unread");
            msg.setId(UUID.randomUUID());
            when(chatMessageRepository.findByConversationIdAndReceiverIdAndReadStatusFalse(
                    any(), eq(testUser2Id))).thenReturn(List.of(msg));

            List<ChatMessage> result = chatService.getUnreadMessages(testUser2Id, testUser1Id);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should throw when IDs are null")
        void shouldThrowWhenIdsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> chatService.getUnreadMessages(null, testUser1Id));
        }
    }

    // ─── Get Conversations ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Get Conversations")
    class GetConversations {

        @Test
        @DisplayName("Should return conversations from conversation table")
        void shouldReturnConversationsFromTable() {
            UUID convId = UUID.randomUUID();
            Conversation conv = conversation(convId, testUser1Id, testUser2Id, "User2", 3);
            when(conversationRepository.findByUserIdOrderByLastMessageAtDesc(testUser1Id))
                    .thenReturn(List.of(conv));

            List<ConversationDTO> result = chatService.getConversations(testUser1Id);

            assertEquals(1, result.size());
            assertEquals(testUser2Id, result.get(0).getUserId());
            assertEquals("User2", result.get(0).getProfileName());
            assertEquals(3, result.get(0).getUnreadCount());
            assertNotNull(result.get(0).getLastMessage());
            // No chatMessageRepository calls — all data comes from conversations table
            verifyNoInteractions(chatMessageRepository);
        }

        @Test
        @DisplayName("Should return empty list when no conversations exist")
        void shouldReturnEmptyList() {
            when(conversationRepository.findByUserIdOrderByLastMessageAtDesc(testUser1Id))
                    .thenReturn(Collections.emptyList());

            List<ConversationDTO> result = chatService.getConversations(testUser1Id);

            assertTrue(result.isEmpty());
            verifyNoInteractions(chatMessageRepository);
        }

        @Test
        @DisplayName("Should filter out blocked partners")
        void shouldFilterBlockedPartners() {
            UUID convId = UUID.randomUUID();
            Conversation conv = conversation(convId, testUser1Id, testUser2Id, "User2", 0);
            when(conversationRepository.findByUserIdOrderByLastMessageAtDesc(testUser1Id))
                    .thenReturn(List.of(conv));
            when(userBlockService.getCombinedBlockedUserIds(testUser1Id))
                    .thenReturn(Set.of(testUser2Id));

            List<ConversationDTO> result = chatService.getConversations(testUser1Id);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should sort by lastMessageAt descending — most recent first")
        void shouldSortByLastMessageAtDescending() {
            UUID conv1Id = UUID.randomUUID();
            UUID conv2Id = UUID.randomUUID();
            UUID user3Id = UUID.randomUUID();

            Conversation older = conversation(conv1Id, testUser1Id, testUser2Id, "User2", 0);
            older.setLastMessageAt(OffsetDateTime.now().minusHours(2));

            Conversation newer = conversation(conv2Id, testUser1Id, user3Id, "User3", 1);
            newer.setLastMessageAt(OffsetDateTime.now());

            // Repository returns older first — service must sort
            when(conversationRepository.findByUserIdOrderByLastMessageAtDesc(testUser1Id))
                    .thenReturn(List.of(newer, older)); // already ordered by DB

            List<ConversationDTO> result = chatService.getConversations(testUser1Id);

            assertEquals(2, result.size());
            assertEquals(user3Id, result.get(0).getUserId(), "Most recent conversation first");
        }

        @Test
        @DisplayName("Should throw when userId is null")
        void shouldThrowWhenUserIdNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> chatService.getConversations(null));
        }
    }
}