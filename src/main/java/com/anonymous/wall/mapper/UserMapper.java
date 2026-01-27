package com.anonymous.wall.mapper;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.UserDTO;
import jakarta.inject.Singleton;

import java.time.ZonedDateTime;
import java.util.UUID;

@Singleton
public class UserMapper {

    public UserDTO toDTO(UserEntity entity) {
        if (entity == null) return null;
        UserDTO dto = new UserDTO();
        dto.setId(entity.getId() != null ? entity.getId().toString() : null);
        dto.setEmail(entity.getEmail());
        dto.setIsVerified(entity.isVerified());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public UserEntity toEntity(UserDTO dto) {
        if (dto == null) return null;

        UserEntity entity = new UserEntity();

        // 只在「更新」场景下才 setId
        if (dto.getId() != null && !dto.getId().isBlank()) {
            entity.setId(UUID.fromString(dto.getId()));
        }

        entity.setEmail(dto.getEmail());
        entity.setVerified(Boolean.TRUE.equals(dto.getIsVerified()));
        entity.setPasswordSet(false);
        entity.setCreatedAt(
                dto.getCreatedAt() != null ? dto.getCreatedAt() : ZonedDateTime.now()
        );

        return entity;
    }

//    public void updatePassword(UserEntity entity, String passwordHash) {
//        if (entity != null && passwordHash != null) {
//            entity.setPasswordHash(passwordHash);
//            entity.setPasswordSet(true);
//        }
//    }
}