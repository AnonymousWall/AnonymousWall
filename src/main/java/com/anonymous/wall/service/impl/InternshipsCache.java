package com.anonymous.wall.service.impl;

import com.anonymous.wall.entity.Internship;
import com.anonymous.wall.repository.InternshipRepository;
import io.micronaut.cache.SyncCache;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Singleton
public class InternshipsCache {

    private static final Logger log = LoggerFactory.getLogger(InternshipsCache.class);

    @Inject
    private InternshipRepository internshipRepository;

    @Inject
    @Named("national-internships")
    private SyncCache<Object> cache;

    /**
     * schoolDomain=null → national wall
     * schoolDomain="harvard.edu" → campus wall for that school
     */
    public Page<Internship> get(int page, int size, String sortBy, String schoolDomain) {
        String key = page + "_" + size + "_" + sortBy.toLowerCase()
                + "_" + (schoolDomain != null ? schoolDomain : "national");
        try {
            Optional<Page> cached = cache.get(key, Page.class);
            if (cached.isPresent()) {
                log.debug("Cache hit for internships: key={}", key);
                //noinspection unchecked
                return (Page<Internship>) cached.get();
            }
        } catch (Exception e) {
            log.warn("Cache get failed for key={}, falling through to DB: {}", key, e.getMessage());
        }

        log.debug("Cache miss for internships: key={}", key);
        Page<Internship> result = fetchFromDb(page, size, sortBy, schoolDomain);

        try {
            cache.put(key, result);
        } catch (Exception e) {
            log.warn("Cache put failed for key={}: {}", key, e.getMessage());
        }

        return result;
    }

    public void invalidateAll() {
        log.debug("Invalidating internships cache");
        cache.invalidateAll();
    }

    private Page<Internship> fetchFromDb(int page, int size, String sortBy, String schoolDomain) {
        Pageable pageable = Pageable.from(page, size);
        if (schoolDomain != null) {
            return switch (sortBy.toLowerCase()) {
                case "oldest" -> internshipRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtAsc("campus", schoolDomain, pageable);
                default       -> internshipRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtDesc("campus", schoolDomain, pageable);
            };
        } else {
            return switch (sortBy.toLowerCase()) {
                case "oldest" -> internshipRepository.findByWallAndHiddenFalseOrderByCreatedAtAsc("national", pageable);
                default       -> internshipRepository.findByWallAndHiddenFalseOrderByCreatedAtDesc("national", pageable);
            };
        }
    }
}