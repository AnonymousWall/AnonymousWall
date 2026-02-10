package com.anonymous.wall.service;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.entity.ChatRoom;
import com.anonymous.wall.entity.RoomMember;
import com.anonymous.wall.entity.RoomMemberId;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.ChatMessageRepository;
import com.anonymous.wall.repository.ChatRoomRepository;
import com.anonymous.wall.repository.RoomMemberRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing chat functionality
 */
@Singleton
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Inject
    private ChatRoomRepository roomRepository;

    @Inject
    private ChatMessageRepository messageRepository;

    @Inject
    private RoomMemberRepository memberRepository;

    @Inject
    private UserRepository userRepository;

    /**
     * Create a new one-to-one chat room between two users
     */
    @Transactional
    public ChatRoom createDirectRoom(UUID user1Id, UUID user2Id) {
        log.info("Creating direct chat room: user1={}, user2={}", user1Id, user2Id);

        // Check if a room already exists between these two users
        Optional<ChatRoom> existingRoom = findExistingDirectRoom(user1Id, user2Id);
        if (existingRoom.isPresent()) {
            log.info("Direct chat room already exists: id={}", existingRoom.get().getId());
            return existingRoom.get();
        }

        // Create new room
        ChatRoom room = new ChatRoom();
        room.setCreatedBy(user1Id);
        room.setCreatedAt(ZonedDateTime.now());
        room.setUpdatedAt(ZonedDateTime.now());

        ChatRoom savedRoom = roomRepository.save(room);

        // Add both users as members
        addMember(savedRoom.getId(), user1Id);
        addMember(savedRoom.getId(), user2Id);

        log.info("Direct chat room created: id={}", savedRoom.getId());
        return savedRoom;
    }

    /**
     * Find existing direct room between two users
     */
    private Optional<ChatRoom> findExistingDirectRoom(UUID user1Id, UUID user2Id) {
        // Get all rooms for user1
        List<RoomMember> user1Rooms = memberRepository.findByUserId(user1Id);
        
        // Check each room to see if user2 is also a member
        for (RoomMember membership : user1Rooms) {
            UUID roomId = membership.getId().getRoomId();
            List<RoomMember> roomMembers = memberRepository.findByRoomId(roomId);
            
            // Direct rooms should have exactly 2 members
            if (roomMembers.size() == 2) {
                boolean hasUser2 = roomMembers.stream()
                    .anyMatch(m -> m.getId().getUserId().equals(user2Id));
                
                if (hasUser2) {
                    return roomRepository.findById(roomId);
                }
            }
        }
        
        return Optional.empty();
    }

    /**
     * Add a member to a room
     */
    @Transactional
    public void addMember(UUID roomId, UUID userId) {
        log.info("Adding member to room: roomId={}, userId={}", roomId, userId);

        RoomMemberId id = new RoomMemberId(roomId, userId);
        
        // Check if already a member
        if (memberRepository.existsById(id)) {
            log.debug("User is already a member of this room");
            return;
        }

        RoomMember member = new RoomMember(id);
        member.setJoinedAt(ZonedDateTime.now());
        memberRepository.save(member);

        log.info("Member added to room successfully");
    }

    /**
     * Send a message to a room
     */
    @Transactional
    public ChatMessage sendMessage(UUID roomId, UUID userId, String content) {
        log.info("Sending message to room: roomId={}, userId={}", roomId, userId);

        // Verify room exists
        Optional<ChatRoom> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            log.error("Room not found: {}", roomId);
            throw new IllegalArgumentException("Room not found");
        }

        // Verify user is a member
        if (!isMember(roomId, userId)) {
            log.error("User is not a member of room: userId={}, roomId={}", userId, roomId);
            throw new SecurityException("User is not a member of this room");
        }

        // Get user's profile name
        String profileName = getUserProfileName(userId);

        // Create message
        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setUserId(userId);
        message.setProfileName(profileName);
        message.setContent(content);
        message.setCreatedAt(ZonedDateTime.now());

        ChatMessage savedMessage = messageRepository.save(message);
        log.info("Message sent successfully: messageId={}", savedMessage.getId());

        return savedMessage;
    }

    /**
     * Get recent messages for a room
     */
    public List<ChatMessage> getRecentMessages(UUID roomId, int limit) {
        log.debug("Getting recent messages: roomId={}, limit={}", roomId, limit);
        return messageRepository.findRecentMessages(roomId, limit);
    }

    /**
     * Get messages with pagination
     */
    public List<ChatMessage> getMessages(UUID roomId, int limit, int offset) {
        log.debug("Getting messages: roomId={}, limit={}, offset={}", roomId, limit, offset);
        return messageRepository.findMessagesWithPagination(roomId, limit, offset);
    }

    /**
     * Get user's rooms
     */
    public List<ChatRoom> getUserRooms(UUID userId) {
        log.debug("Getting user's rooms: userId={}", userId);
        
        List<RoomMember> memberships = memberRepository.findByUserId(userId);
        List<UUID> roomIds = memberships.stream()
            .map(m -> m.getId().getRoomId())
            .toList();

        return (List<ChatRoom>) roomRepository.findAllById(roomIds);
    }

    /**
     * Check if user can access a room
     */
    public boolean canAccessRoom(UUID userId, UUID roomId) {
        return isMember(roomId, userId);
    }

    /**
     * Check if user is a member of a room
     */
    public boolean isMember(UUID roomId, UUID userId) {
        RoomMemberId id = new RoomMemberId(roomId, userId);
        return memberRepository.existsById(id);
    }

    /**
     * Get room by ID
     */
    public Optional<ChatRoom> getRoom(UUID roomId) {
        return roomRepository.findById(roomId);
    }

    /**
     * Delete a message (soft delete)
     */
    @Transactional
    public void deleteMessage(UUID messageId, UUID userId) {
        log.info("Deleting message: messageId={}, userId={}", messageId, userId);

        Optional<ChatMessage> messageOpt = messageRepository.findById(messageId);
        if (messageOpt.isEmpty()) {
            log.error("Message not found: {}", messageId);
            throw new IllegalArgumentException("Message not found");
        }

        ChatMessage message = messageOpt.get();

        // Verify user owns the message
        if (!message.getUserId().equals(userId)) {
            log.error("User does not own message: userId={}, messageOwnerId={}", userId, message.getUserId());
            throw new SecurityException("You can only delete your own messages");
        }

        message.setDeleted(true);
        messageRepository.update(message);

        log.info("Message deleted successfully");
    }

    /**
     * Get members of a room
     */
    public List<RoomMember> getRoomMembers(UUID roomId) {
        return memberRepository.findByRoomId(roomId);
    }

    /**
     * Update last read timestamp for a user in a room
     */
    @Transactional
    public void updateLastRead(UUID roomId, UUID userId) {
        RoomMemberId id = new RoomMemberId(roomId, userId);
        Optional<RoomMember> memberOpt = memberRepository.findById(id);
        
        if (memberOpt.isPresent()) {
            RoomMember member = memberOpt.get();
            member.setLastReadAt(ZonedDateTime.now());
            memberRepository.update(member);
            log.debug("Updated last read time: roomId={}, userId={}", roomId, userId);
        }
    }

    /**
     * Get user's profile name
     */
    private String getUserProfileName(UUID userId) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            String profileName = userOpt.get().getProfileName();
            return profileName != null ? profileName : "Anonymous";
        }
        return "Anonymous";
    }
}
