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

    Page<MarketplaceItem> findAllOrderByCreatedAtDesc(Pageable pageable);

    Page<MarketplaceItem> findAllOrderByPriceAsc(Pageable pageable);

    Page<MarketplaceItem> findAllOrderByPriceDesc(Pageable pageable);

    // Wall-based queries (same pattern as Posts)
    Page<MarketplaceItem> findByWallAndHiddenFalseOrderByCreatedAtDesc(String wall, Pageable pageable);

    Page<MarketplaceItem> findByWallAndHiddenFalseOrderByCreatedAtAsc(String wall, Pageable pageable);

    Page<MarketplaceItem> findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtDesc(String wall, String schoolDomain, Pageable pageable);

    Page<MarketplaceItem> findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtAsc(String wall, String schoolDomain, Pageable pageable);

    Page<MarketplaceItem> findByWallAndHiddenFalseOrderByPriceAsc(String wall, Pageable pageable);

    Page<MarketplaceItem> findByWallAndHiddenFalseOrderByPriceDesc(String wall, Pageable pageable);

    Page<MarketplaceItem> findByWallAndSchoolDomainAndHiddenFalseOrderByPriceAsc(String wall, String schoolDomain, Pageable pageable);

    Page<MarketplaceItem> findByWallAndSchoolDomainAndHiddenFalseOrderByPriceDesc(String wall, String schoolDomain, Pageable pageable);

    // Wall + sold filtered queries
    Page<MarketplaceItem> findByWallAndSoldAndHiddenFalseOrderByCreatedAtDesc(String wall, boolean sold, Pageable pageable);

    Page<MarketplaceItem> findByWallAndSoldAndHiddenFalseOrderByCreatedAtAsc(String wall, boolean sold, Pageable pageable);

    Page<MarketplaceItem> findByWallAndSoldAndHiddenFalseOrderByPriceAsc(String wall, boolean sold, Pageable pageable);

    Page<MarketplaceItem> findByWallAndSoldAndHiddenFalseOrderByPriceDesc(String wall, boolean sold, Pageable pageable);

    Page<MarketplaceItem> findByWallAndSchoolDomainAndSoldAndHiddenFalseOrderByCreatedAtDesc(String wall, String schoolDomain, boolean sold, Pageable pageable);

    Page<MarketplaceItem> findByWallAndSchoolDomainAndSoldAndHiddenFalseOrderByCreatedAtAsc(String wall, String schoolDomain, boolean sold, Pageable pageable);

    Page<MarketplaceItem> findByWallAndSchoolDomainAndSoldAndHiddenFalseOrderByPriceAsc(String wall, String schoolDomain, boolean sold, Pageable pageable);

    Page<MarketplaceItem> findByWallAndSchoolDomainAndSoldAndHiddenFalseOrderByPriceDesc(String wall, String schoolDomain, boolean sold, Pageable pageable);

    List<MarketplaceItem> findByUserId(UUID userId);

    Page<MarketplaceItem> findByUserId(UUID userId, Pageable pageable);

    Page<MarketplaceItem> findAll(Pageable pageable);

    Page<MarketplaceItem> findByHidden(boolean hidden, Pageable pageable);

    Page<MarketplaceItem> findByUserIdAndHidden(UUID userId, boolean hidden, Pageable pageable);

    Page<MarketplaceItem> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<MarketplaceItem> findByUserIdAndHiddenFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<MarketplaceItem> findByUserIdAndHiddenFalseOrderByCreatedAtAsc(UUID userId, Pageable pageable);

    Optional<MarketplaceItem> findById(UUID id);

    Page<MarketplaceItem> findBySoldOrderByCreatedAtDesc(boolean sold, Pageable pageable);

    Page<MarketplaceItem> findBySoldOrderByPriceAsc(boolean sold, Pageable pageable);

    Page<MarketplaceItem> findBySoldOrderByPriceDesc(boolean sold, Pageable pageable);

    Page<MarketplaceItem> findByCategoryOrderByCreatedAtDesc(String category, Pageable pageable);

    long countByUserId(UUID userId);

    MarketplaceItem update(MarketplaceItem item);

    void updateProfileNameByUserId(UUID userId, String profileName);
}
