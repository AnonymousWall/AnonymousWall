package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.model.AdminConversationDTO;
import com.anonymous.wall.repository.ChatMessageRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Singleton
public class AdminChatServiceImpl implements AdminChatService {

    private static final Logger log = LoggerFactory.getLogger(AdminChatServiceImpl.class);

    @Inject
    private ChatMessageRepository chatMessageRepository;

    @Override
    public Page<AdminConversationDTO> getAllConversations(Pageable pageable, UUID userId) {
        log.info("Admin fetching conversations - userId={}, page={}, size={}", userId, pageable.getNumber(), pageable.getSize());

        int limit = pageable.getSize();
        int offset = (int) pageable.getOffset();

        List<UUID> conversationIds;
        long total;

        if (userId != null) {
            conversationIds = chatMessageRepository.findDistinctConversationIdsByUserIdPaged(userId, limit, offset);
            total = chatMessageRepository.countDistinctConversationsByUserId(userId);
        } else {
            conversationIds = chatMessageRepository.findDistinctConversationIdsPaged(limit, offset);
            total = chatMessageRepository.countDistinctConversations();
        }

        List<AdminConversationDTO> dtos = new ArrayList<>();
        for (UUID convId : conversationIds) {
            AdminConversationDTO dto = buildConversationDTO(convId);
            dtos.add(dto);
        }

        return Page.of(dtos, pageable, total);
    }

    private AdminConversationDTO buildConversationDTO(UUID conversationId) {
        AdminConversationDTO dto = new AdminConversationDTO();
        dto.setConversationId(conversationId);

        chatMessageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId).ifPresent(msg -> {
            dto.setLastMessageAt(msg.getCreatedAt());
            dto.setParticipantIds(Arrays.asList(msg.getSenderId(), msg.getReceiverId()));
        });

        long count = chatMessageRepository.countByConversationId(conversationId);
        dto.setMessageCount((int) count);

        return dto;
    }

    @Override
    public Page<ChatMessage> getConversationMessages(UUID conversationId, Pageable pageable) {
        log.info("Admin fetching messages for conversation: {}", conversationId);
        return chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);
    }
}
