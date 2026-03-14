package com.anonymous.wall.notification.device;

import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.OffsetDateTime;
import java.util.UUID;

@MappedEntity(value = "device_tokens", namingStrategy = NamingStrategies.Raw.class)
public class DeviceToken {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("user_id")
    private UUID userId;

    @MappedProperty("device_token")
    private String deviceToken;

    @MappedProperty("platform")
    private String platform;

    @MappedProperty("created_at")
    private OffsetDateTime createdAt;

    @MappedProperty("updated_at")
    private OffsetDateTime updatedAt;

    @MappedProperty("active")
    private boolean active = true;

    public DeviceToken() {}

    public DeviceToken(UUID userId, String deviceToken, String platform) {
        this.userId = userId;
        this.deviceToken = deviceToken;
        this.platform = platform;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        this.active = true;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}