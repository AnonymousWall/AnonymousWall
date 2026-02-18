package com.anonymous.wall.repository;

import com.anonymous.wall.entity.MarketplaceItem;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface MarketplaceItemRepository extends CrudRepository<MarketplaceItem, UUID> {

    /**
     * Find all items with pagination, sorted by created time (newest first)
     */
    Page<MarketplaceItem> findAllOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find all items with pagination, sorted by price ascending
     */
    Page<MarketplaceItem> findAllOrderByPriceAsc(Pageable pageable);

    /**
     * Find all items with pagination, sorted by price descending
     */
    Page<MarketplaceItem> findAllOrderByPriceDesc(Pageable pageable);

    /**
     * Find items by user ID
     */
    List<MarketplaceItem> findByUserId(UUID userId);

    /**
     * Find items by user ID with pagination
     */
    Page<MarketplaceItem> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find item by ID
     */
    Optional<MarketplaceItem> findById(UUID id);

    /**
     * Find items by sold status
     */
    Page<MarketplaceItem> findBySoldOrderByCreatedAtDesc(boolean sold, Pageable pageable);

    /**
     * Find items by category
     */
    Page<MarketplaceItem> findByCategoryOrderByCreatedAtDesc(String category, Pageable pageable);

    /**
     * Count items by user ID
     */
    long countByUserId(UUID userId);
}
