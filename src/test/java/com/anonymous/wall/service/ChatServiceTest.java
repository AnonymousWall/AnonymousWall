package com.anonymous.wall.service;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.ConversationDTO;
import com.anonymous.wall.repository.ChatMessageRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ChatService.
 * Tests message sending, validation, blocked user checks, and conversation management.
 */
@DisplayName("ChatService Unit Tests")
class ChatServiceTest {

    private ChatServiceImpl chatService;
    private ChatMessageRepository chatMessageRepository;
    private UserRepository userRepository;

    private UUID testUser1Id;
    private UUID testUser2Id;
    private UUID blockedUserId;
    private UserEntity testUser1;
    private UserEntity testUser2;
    private UserEntity blockedUser;

    @BeforeEach
    void setUp() {
        chatMessageRepository = mock(ChatMessageRepository.class);
        userRepository = mock(UserRepository.class);
        
        chatService = new ChatServiceImpl();
        
        try {
            var chatRepoField = ChatServiceImpl.class.getDeclaredField("chatMessageRepository");
            chatRepoField.setAccessible(true);
            chatRepoField.set(chatService, chatMessageRepository);

            var userRepoField = ChatServiceImpl.class.getDeclaredField("userRepository");
            userRepoField.setAccessible(true);
            userRepoField.set(chatService, userRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Setup test data
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
    }

    @Nested
    @DisplayName("Send Message - Positive Cases")
    class SendMessagePositiveCases {

        @Test
        @DisplayName("Should send message successfully between two valid users")
        void shouldSendMessageSuccessfully() {
            // Arrange
            String content = "Hello, this is a test message!";
            
            when(userRepository.findById(testUser1Id)).thenReturn(Optional.of(testUser1));
            when(userRepository.findById(testUser2Id)).thenReturn(Optional.of(testUser2));
            when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
                ChatMessage message = invocation.getArgument(0);
                message.setId(UUID.randomUUID());
                return message;
            });

            // Act
            ChatMessage result = chatService.sendMessage(testUser1Id, testUser2Id, content);

            // Assert
            assertNotNull(result);
            assertEquals(content, result.getContent());
            assertEquals(testUser1Id, result.getSenderId());
            assertEquals(testUser2Id, result.getReceiverId());
            assertNotNull(result.getConversationId(), "Conversation ID should be set");
            assertFalse(result.isReadStatus());
            assertNotNull(result.getCreatedAt());
            verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should trim message content")
        void shouldTrimMessageContent() {
            // Arrange
            String content = "  Hello with spaces  ";
            
            when(userRepository.findById(testUser1Id)).thenReturn(Optional.of(testUser1));
            when(userRepository.findById(testUser2Id)).thenReturn(Optional.of(testUser2));
            when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
                ChatMessage message = invocation.getArgument(0);
                message.setId(UUID.randomUUID());
                return message;
            });

            // Act
            ChatMessage result = chatService.sendMessage(testUser1Id, testUser2Id, content);

            // Assert
            assertEquals("Hello with spaces", result.getContent());
        }
    }

    @Nested
    @DisplayName("Send Message - Validation Errors")
    class SendMessageValidationErrors {

        @Test
        @DisplayName("Should throw exception when sender ID is null")
        void shouldThrowExceptionWhenSenderIdIsNull() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatService.sendMessage(null, testUser2Id, "Test message")
            );
            
            assertEquals("Sender ID and receiver ID must not be null", exception.getMessage());
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when receiver ID is null")
        void shouldThrowExceptionWhenReceiverIdIsNull() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatService.sendMessage(testUser1Id, null, "Test message")
            );
            
            assertEquals("Sender ID and receiver ID must not be null", exception.getMessage());
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when content is null")
        void shouldThrowExceptionWhenContentIsNull() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatService.sendMessage(testUser1Id, testUser2Id, null)
            );
            
            assertEquals("Message content must not be empty", exception.getMessage());
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when content is empty")
        void shouldThrowExceptionWhenContentIsEmpty() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatService.sendMessage(testUser1Id, testUser2Id, "")
            );
            
            assertEquals("Message content must not be empty", exception.getMessage());
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when content is only whitespace")
        void shouldThrowExceptionWhenContentIsOnlyWhitespace() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatService.sendMessage(testUser1Id, testUser2Id, "   ")
            );
            
            assertEquals("Message content must not be empty", exception.getMessage());
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when content exceeds 5000 characters")
        void shouldThrowExceptionWhenContentTooLong() {
            // Arrange
            String longContent = "a".repeat(5001);
            
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatService.sendMessage(testUser1Id, testUser2Id, longContent)
            );
            
            assertEquals("Message content exceeds maximum length of 5000 characters", exception.getMessage());
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when sender not found")
        void shouldThrowExceptionWhenSenderNotFound() {
            // Arrange
            when(userRepository.findById(testUser1Id)).thenReturn(Optional.empty());
            
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatService.sendMessage(testUser1Id, testUser2Id, "Test message")
            );
            
            assertEquals("Sender not found", exception.getMessage());
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when receiver not found")
        void shouldThrowExceptionWhenReceiverNotFound() {
            // Arrange
            when(userRepository.findById(testUser1Id)).thenReturn(Optional.of(testUser1));
            when(userRepository.findById(testUser2Id)).thenReturn(Optional.empty());
            
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatService.sendMessage(testUser1Id, testUser2Id, "Test message")
            );
            
            assertEquals("Receiver not found", exception.getMessage());
            verify(chatMessageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Send Message - Blocked User Checks")
    class SendMessageBlockedUserChecks {

        @Test
        @DisplayName("Should throw exception when receiver is blocked")
        void shouldThrowExceptionWhenReceiverIsBlocked() {
            // Arrange
            when(userRepository.findById(testUser1Id)).thenReturn(Optional.of(testUser1));
            when(userRepository.findById(blockedUserId)).thenReturn(Optional.of(blockedUser));
            
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatService.sendMessage(testUser1Id, blockedUserId, "Test message")
            );
            
            assertEquals("Cannot send message to a blocked user", exception.getMessage());
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when sender is blocked")
        void shouldThrowExceptionWhenSenderIsBlocked() {
            // Arrange
            when(userRepository.findById(blockedUserId)).thenReturn(Optional.of(blockedUser));
            when(userRepository.findById(testUser2Id)).thenReturn(Optional.of(testUser2));
            
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatService.sendMessage(blockedUserId, testUser2Id, "Test message")
            );
            
            assertEquals("Blocked users cannot send messages", exception.getMessage());
            verify(chatMessageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Message History")
    class GetMessageHistory {

        @Test
        @DisplayName("Should get message history successfully")
        void shouldGetMessageHistorySuccessfully() {
            // Arrange
            Pageable pageable = Pageable.from(0, 50);
            Page<ChatMessage> mockPage = mock(Page.class);
            
            when(userRepository.existsById(testUser1Id)).thenReturn(true);
            when(userRepository.existsById(testUser2Id)).thenReturn(true);
            when(chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(any(UUID.class), eq(pageable)))
                .thenReturn(mockPage);

            // Act
            Page<ChatMessage> result = chatService.getMessageHistory(testUser1Id, testUser2Id, pageable);

            // Assert
            assertNotNull(result);
            verify(chatMessageRepository, times(1))
                .findByConversationIdOrderByCreatedAtAsc(any(UUID.class), eq(pageable));
        }

        @Test
        @DisplayName("Should throw exception when user1 ID is null")
        void shouldThrowExceptionWhenUser1IdIsNull() {
            // Arrange
            Pageable pageable = Pageable.from(0, 50);
            
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatService.getMessageHistory(null, testUser2Id, pageable)
            );
            
            assertEquals("User IDs must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when user1 not found")
        void shouldThrowExceptionWhenUser1NotFound() {
            // Arrange
            Pageable pageable = Pageable.from(0, 50);
            when(userRepository.existsById(testUser1Id)).thenReturn(false);
            
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatService.getMessageHistory(testUser1Id, testUser2Id, pageable)
            );
            
            assertTrue(exception.getMessage().contains("User not found"));
        }
    }

    @Nested
    @DisplayName("Mark Message As Read")
    class MarkMessageAsRead {

        @Test
        @DisplayName("Should mark message as read successfully")
        void shouldMarkMessageAsReadSuccessfully() {
            // Arrange
            UUID messageId = UUID.randomUUID();
            ChatMessage message = new ChatMessage(testUser1Id, testUser2Id, "Test");
            message.setId(messageId);
            message.setReadStatus(false);
            
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));
            when(chatMessageRepository.update(any(ChatMessage.class))).thenReturn(message);

            // Act
            chatService.markMessageAsRead(messageId, testUser2Id);

            // Assert
            verify(chatMessageRepository, times(1)).update(any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should not update if message already read")
        void shouldNotUpdateIfMessageAlreadyRead() {
            // Arrange
            UUID messageId = UUID.randomUUID();
            ChatMessage message = new ChatMessage(testUser1Id, testUser2Id, "Test");
            message.setId(messageId);
            message.setReadStatus(true);
            
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));

            // Act
            chatService.markMessageAsRead(messageId, testUser2Id);

            // Assert
            verify(chatMessageRepository, never()).update(any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should throw exception when user is not the receiver")
        void shouldThrowExceptionWhenUserIsNotReceiver() {
            // Arrange
            UUID messageId = UUID.randomUUID();
            ChatMessage message = new ChatMessage(testUser1Id, testUser2Id, "Test");
            message.setId(messageId);
            
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatService.markMessageAsRead(messageId, testUser1Id)
            );
            
            assertEquals("Only the receiver can mark a message as read", exception.getMessage());
            verify(chatMessageRepository, never()).update(any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should throw exception when message not found")
        void shouldThrowExceptionWhenMessageNotFound() {
            // Arrange
            UUID messageId = UUID.randomUUID();
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatService.markMessageAsRead(messageId, testUser2Id)
            );
            
            assertEquals("Message not found", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Mark Conversation As Read")
    class MarkConversationAsRead {

        @Test
        @DisplayName("Should mark all messages in conversation as read")
        void shouldMarkAllMessagesAsRead() {
            // Arrange
            doNothing().when(chatMessageRepository).markConversationMessagesAsRead(any(UUID.class), eq(testUser2Id));

            // Act
            chatService.markConversationAsRead(testUser2Id, testUser1Id);

            // Assert
            verify(chatMessageRepository, times(1)).markConversationMessagesAsRead(any(UUID.class), eq(testUser2Id));
        }

        @Test
        @DisplayName("Should throw exception when receiver ID is null")
        void shouldThrowExceptionWhenReceiverIdIsNull() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatService.markConversationAsRead(null, testUser1Id)
            );
            
            assertEquals("Receiver ID and sender ID must not be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Count Unread Messages")
    class CountUnreadMessages {

        @Test
        @DisplayName("Should count unread messages from specific sender")
        void shouldCountUnreadMessagesFromSender() {
            // Arrange
            when(chatMessageRepository.countByConversationIdAndReceiverIdAndReadStatusFalse(any(UUID.class), eq(testUser2Id)))
                .thenReturn(5L);

            // Act
            long count = chatService.countUnreadMessages(testUser2Id, testUser1Id);

            // Assert
            assertEquals(5L, count);
        }

        @Test
        @DisplayName("Should count total unread messages for receiver")
        void shouldCountTotalUnreadMessages() {
            // Arrange
            when(chatMessageRepository.countByReceiverIdAndReadStatusFalse(testUser2Id))
                .thenReturn(10L);

            // Act
            long count = chatService.countTotalUnreadMessages(testUser2Id);

            // Assert
            assertEquals(10L, count);
        }
    }

    @Nested
    @DisplayName("Get Conversations")
    class GetConversations {

        @Test
        @DisplayName("Should get list of conversations successfully")
        void shouldGetConversationsSuccessfully() {
            // Arrange
            UUID conversationId = UUID.randomUUID();
            List<UUID> conversationIds = Arrays.asList(conversationId);
            when(chatMessageRepository.findUserConversations(testUser1Id)).thenReturn(conversationIds);
            when(chatMessageRepository.findOtherParticipantInConversation(conversationId, testUser1Id)).thenReturn(testUser2Id);
            when(userRepository.findById(testUser2Id)).thenReturn(Optional.of(testUser2));
            
            ChatMessage lastMessage = new ChatMessage(testUser2Id, testUser1Id, "Last message");
            lastMessage.setId(UUID.randomUUID());
            lastMessage.setCreatedAt(OffsetDateTime.now());
            when(chatMessageRepository.findLastMessageInConversation(conversationId))
                .thenReturn(lastMessage);
            
            when(chatMessageRepository.countByConversationIdAndReceiverIdAndReadStatusFalse(conversationId, testUser1Id))
                .thenReturn(3L);

            // Act
            List<ConversationDTO> conversations = chatService.getConversations(testUser1Id);

            // Assert
            assertNotNull(conversations);
            assertEquals(1, conversations.size());
            assertEquals(testUser2Id, conversations.get(0).getUserId());
            assertEquals("User2", conversations.get(0).getProfileName());
            assertEquals(3, conversations.get(0).getUnreadCount());
            assertNotNull(conversations.get(0).getLastMessage());
        }

        @Test
        @DisplayName("Should skip conversation if partner user not found")
        void shouldSkipIfPartnerNotFound() {
            // Arrange
            UUID conversationId = UUID.randomUUID();
            List<UUID> conversationIds = Arrays.asList(conversationId);
            when(chatMessageRepository.findUserConversations(testUser1Id)).thenReturn(conversationIds);
            when(chatMessageRepository.findOtherParticipantInConversation(conversationId, testUser1Id)).thenReturn(testUser2Id);
            when(userRepository.findById(testUser2Id)).thenReturn(Optional.empty());

            // Act
            List<ConversationDTO> conversations = chatService.getConversations(testUser1Id);

            // Assert
            assertNotNull(conversations);
            assertEquals(0, conversations.size());
        }
    }
}
