package com.anonymous.wall.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Utility class for generating deterministic conversation IDs.
 * Ensures that a conversation between two users always has the same ID,
 * regardless of who initiates the conversation or the order of user IDs.
 * 
 * This approach eliminates the need for OR-based queries in the database,
 * improving query performance and enabling better indexing and pagination.
 */
public final class ConversationIdGenerator {

    private ConversationIdGenerator() {
        // Prevent instantiation
    }

    /**
     * Generates a deterministic conversation ID for two users.
     * The same conversation ID is generated regardless of the order of user IDs.
     * 
     * Algorithm:
     * 1. Sort the two UUIDs to ensure consistent ordering
     * 2. Combine them into a string with a delimiter
     * 3. Generate a UUID v3 (name-based) from the combined string
     * 
     * @param user1 First user's UUID
     * @param user2 Second user's UUID
     * @return A deterministic UUID representing the conversation between the two users
     * @throws IllegalArgumentException if either user ID is null
     */
    public static UUID generate(UUID user1, UUID user2) {
        if (user1 == null || user2 == null) {
            throw new IllegalArgumentException("User IDs must not be null");
        }

        // Sort UUIDs to ensure deterministic ordering
        UUID min = user1.compareTo(user2) < 0 ? user1 : user2;
        UUID max = user1.compareTo(user2) < 0 ? user2 : user1;

        // Combine into a deterministic string
        String combined = min.toString() + ":" + max.toString();

        // Generate UUID v3 (name-based) from the combined string
        return UUID.nameUUIDFromBytes(combined.getBytes(StandardCharsets.UTF_8));
    }
}
