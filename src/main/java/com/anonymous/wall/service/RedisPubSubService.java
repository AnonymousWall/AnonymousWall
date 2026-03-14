package com.anonymous.wall.service;

import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Service for Redis Pub/Sub messaging to support WebSocket broadcasting
 * across multiple backend instances.
 */
@Singleton
public class RedisPubSubService {

    private static final Logger log = LoggerFactory.getLogger(RedisPubSubService.class);
    private static final String CHANNEL_PREFIX = "user:";

    @Inject
    StatefulRedisConnection<String, String> redisConnection;

    @Inject
    StatefulRedisPubSubConnection<String, String> redisPubSubConnection;

    // Track listeners per userId so we can remove them on unsubscribe
    private final Map<UUID, RedisPubSubAdapter<String, String>> listeners = new ConcurrentHashMap<>();

    /**
     * Publish a message to a user's Redis channel.
     * Called by whichever instance receives the outbound message.
     */
    public void publish(UUID userId, String messageJson) {
        redisConnection.async().publish(channelFor(userId), messageJson)
                .exceptionally(ex -> {
                    log.error("Failed to publish Redis message for user {}: {}", userId, ex.getMessage());
                    return null;
                });
    }

    /**
     * Subscribe to a user's channel.
     * Called when a user connects their WebSocket on this instance.
     */
    public synchronized void subscribe(UUID userId, Consumer<String> onMessage) {
        // Evict any stale listener first to avoid orphaned callbacks on reconnect
        RedisPubSubAdapter<String, String> stale = listeners.remove(userId);
        if (stale != null) {
            redisPubSubConnection.removeListener(stale);
        }

        RedisPubSubAdapter<String, String> listener = new RedisPubSubAdapter<>() {
            @Override
            public void message(String channel, String message) {
                try {
                    onMessage.accept(message);
                } catch (Exception e) {
                    log.error("Error in Redis subscriber callback for user {}: {}", userId, e.getMessage());
                }
            }
        };

        listeners.put(userId, listener);
        redisPubSubConnection.addListener(listener);
        redisPubSubConnection.async().subscribe(channelFor(userId));
        log.debug("Subscribed to Redis channel for user {}", userId);
    }

    /**
     * Unsubscribe from a user's channel.
     * Called when all WebSocket sessions for a user disconnect from this instance.
     */
    public synchronized void unsubscribe(UUID userId) {
        RedisPubSubAdapter<String, String> listener = listeners.remove(userId);
        if (listener != null) {
            redisPubSubConnection.removeListener(listener);
            redisPubSubConnection.async().unsubscribe(channelFor(userId));
            log.debug("Unsubscribed from Redis channel for user {}", userId);
        }
    }

    private String channelFor(UUID userId) {
        return CHANNEL_PREFIX + userId;
    }
}