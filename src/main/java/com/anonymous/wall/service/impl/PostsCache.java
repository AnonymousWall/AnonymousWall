package com.anonymous.wall.service.impl;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.repository.PostRepository;
import io.micronaut.cache.SyncCache;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Singleton
public class PostsCache {

    private static final Logger log = LoggerFactory.getLogger(PostsCache.class);

    @Inject
    private PostRepository postRepository;

    @Inject
    @jakarta.inject.Named("national-posts")
    private SyncCache<Object> cache;

    /**
     * schoolDomain=null → national wall
     * schoolDomain="harvard.edu" → campus wall for that school
     */
    public Page<Post> get(int page, int size, SortBy sortBy, String schoolDomain) {
        String key = page + "_" + size + "_" + sortBy.name() + "_" + (schoolDomain != null ? schoolDomain : "national");
        try {
            Optional<Page> cached = cache.get(key, Page.class);
            if (cached.isPresent()) {
                log.debug("Cache hit: key={}", key);
                //noinspection unchecked
                return (Page<Post>) cached.get();
            }
        } catch (Exception e) {
            log.warn("Cache get failed for key={}, falling through to DB: {}", key, e.getMessage());
        }

        log.debug("Cache miss: key={}", key);
        Page<Post> result = fetchFromDb(page, size, sortBy, schoolDomain);

        try {
            cache.put(key, result);
        } catch (Exception e) {
            log.warn("Cache put failed for key={}: {}", key, e.getMessage());
        }

        return result;
    }

    public void invalidateAll() {
        log.debug("Invalidating posts cache");
        cache.invalidateAll();
    }

    private Page<Post> fetchFromDb(int page, int size, SortBy sortBy, String schoolDomain) {
        Pageable pageable = Pageable.from(page, size);
        if (schoolDomain == null) {
            return switch (sortBy) {
                case NEWEST        -> postRepository.findByWallAndHiddenFalseOrderByCreatedAtDesc("national", pageable);
                case OLDEST        -> postRepository.findByWallAndHiddenFalseOrderByCreatedAtAsc("national", pageable);
                case MOST_LIKED    -> postRepository.findByWallAndHiddenFalseOrderByLikeCountDesc("national", pageable);
                case LEAST_LIKED   -> postRepository.findByWallAndHiddenFalseOrderByLikeCountAsc("national", pageable);
                case MOST_COMMENTED  -> postRepository.findByWallAndHiddenFalseOrderByCommentCountDesc("national", pageable);
                case LEAST_COMMENTED -> postRepository.findByWallAndHiddenFalseOrderByCommentCountAsc("national", pageable);
            };
        } else {
            return switch (sortBy) {
                case NEWEST        -> postRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtDesc("campus", schoolDomain, pageable);
                case OLDEST        -> postRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtAsc("campus", schoolDomain, pageable);
                case MOST_LIKED    -> postRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByLikeCountDesc("campus", schoolDomain, pageable);
                case LEAST_LIKED   -> postRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByLikeCountAsc("campus", schoolDomain, pageable);
                case MOST_COMMENTED  -> postRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByCommentCountDesc("campus", schoolDomain, pageable);
                case LEAST_COMMENTED -> postRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByCommentCountAsc("campus", schoolDomain, pageable);
            };
        }
    }
}