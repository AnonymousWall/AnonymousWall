package com.anonymous.wall.event;

import java.util.UUID;

/**
 * Event fired when a marketplace item is hidden.
 * This event triggers asynchronous updates to hide all comments associated with the item.
 */
public class MarketplaceItemHiddenEvent {

    private final UUID itemId;
    private final UUID userId;

    public MarketplaceItemHiddenEvent(UUID itemId, UUID userId) {
        this.itemId = itemId;
        this.userId = userId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return "MarketplaceItemHiddenEvent{" +
                "itemId=" + itemId +
                ", userId=" + userId +
                '}';
    }
}
