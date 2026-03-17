package com.anonymous.wall.service.base;

import com.anonymous.wall.entity.PostLike;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostLikeService {
    Optional<PostLike> findByPostIdAndUserId(UUID postId, UUID userId);
    void deleteByPostIdAndUserId(UUID postId, UUID userId);
    PostLike save(PostLike postLike);
    List<PostLike> findByUserIdAndPostIdIn(UUID userId, List<UUID> postIds);
    List<PostLike> findByUserId(UUID userId);
}
