package com.anonymous.wall.service.impl;

import com.anonymous.wall.entity.PostLike;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.service.base.PostLikeService;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class PostLikeServiceImpl implements PostLikeService {
    @Inject
    private PostLikeRepository postLikeRepository;

    @Override
    @Transactional
    public Optional<PostLike> findByPostIdAndUserId(UUID postId, UUID userId) {
        return postLikeRepository.findByPostIdAndUserId(postId, userId);
    }

    @Override
    @Transactional
    public void deleteByPostIdAndUserId(UUID postId, UUID userId) {
        postLikeRepository.deleteByPostIdAndUserId(postId, userId);
    }

    @Override
    @Transactional
    public PostLike save(PostLike postLike) {
        return postLikeRepository.save(postLike);
    }

    @Override
    @Transactional
    public List<PostLike> findByUserIdAndPostIdIn(UUID userId, List<UUID> postIds) {
        return postLikeRepository.findByUserIdAndPostIdIn(userId, postIds);
    }
}