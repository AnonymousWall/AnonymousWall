package com.anonymous.wall.entity;

import io.micronaut.data.annotation.Embeddable;
import io.micronaut.data.annotation.MappedProperty;

import java.util.Objects;
import java.util.UUID;

@Embeddable
public class RoomMemberId {

    @MappedProperty("room_id")
    private UUID roomId;

    @MappedProperty("user_id")
    private UUID userId;

    public RoomMemberId() {}

    public RoomMemberId(UUID roomId, UUID userId) {
        this.roomId = roomId;
        this.userId = userId;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoomMemberId that = (RoomMemberId) o;
        return Objects.equals(roomId, that.roomId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, userId);
    }
}
