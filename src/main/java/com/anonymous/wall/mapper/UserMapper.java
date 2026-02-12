package com.anonymous.wall.mapper;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.UserDTO;
import jakarta.inject.Singleton;

import java.time.OffsetDateTime;
import java.util.UUID;

@Singleton
public class UserMapper {

    public UserDTO toDTO(UserEntity entity) {
        if (entity == null) return null;
        UserDTO dto = new UserDTO();
        dto.setId(entity.getId() != null ? entity.getId() : null);
        dto.setEmail(entity.getEmail());
        dto.setProfileName(entity.getProfileName());
        dto.setIsVerified(entity.isVerified());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setPasswordSet(entity.isPasswordSet());
        return dto;
    }

    public UserEntity toEntity(UserDTO dto) {
        if (dto == null) return null;

        UserEntity entity = new UserEntity();

        // 只在「更新」场景下才 setId
        if (dto.getId() != null) {
            entity.setId(dto.getId());
        }

        entity.setEmail(dto.getEmail());
        entity.setVerified(Boolean.TRUE.equals(dto.getIsVerified()));
        entity.setPasswordSet(false);
        entity.setCreatedAt(
                dto.getCreatedAt() != null ? dto.getCreatedAt() : OffsetDateTime.now()
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