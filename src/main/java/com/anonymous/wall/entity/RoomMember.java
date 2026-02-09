package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.ZonedDateTime;
import java.util.UUID;

@MappedEntity(value = "room_members", namingStrategy = NamingStrategies.Raw.class)
public class RoomMember {

    @EmbeddedId
    private RoomMemberId id;

    @MappedProperty("joined_at")
    private ZonedDateTime joinedAt = ZonedDateTime.now();

    @MappedProperty("last_read_at")
    private ZonedDateTime lastReadAt;

    @MappedProperty("is_muted")
    private boolean muted = false;

    // Constructors
    public RoomMember() {}

    public RoomMember(UUID roomId, UUID userId) {
        this.id = new RoomMemberId(roomId, userId);
    }

    public RoomMember(RoomMemberId id) {
        this.id = id;
    }

    // Getters and Setters
    public RoomMemberId getId() {
        return id;
    }

    public void setId(RoomMemberId id) {
        this.id = id;
    }

    public ZonedDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(ZonedDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public ZonedDateTime getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(ZonedDateTime lastReadAt) {
        this.lastReadAt = lastReadAt;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }
}
