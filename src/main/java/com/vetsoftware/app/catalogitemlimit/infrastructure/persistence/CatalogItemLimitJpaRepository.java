package com.vetsoftware.app.catalogitemlimit.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Consultas de los techos de fábrica. Catálogo global: sin filtro de empresa.
 */
public interface CatalogItemLimitJpaRepository
        extends
            JpaRepository<CatalogItemLimitJpaEntity, Long> {

    Optional<CatalogItemLimitJpaEntity> findByIdAndCatalogItemId(Long id, Long catalogItemId);

    Optional<CatalogItemLimitJpaEntity> findByCatalogItemIdAndLimitDimensionId(Long catalogItemId,
            Long limitDimensionId);

    boolean existsByCatalogItemIdAndLimitDimensionId(Long catalogItemId, Long limitDimensionId);

    List<CatalogItemLimitJpaEntity> findAllByCatalogItemIdOrderByLimitDimensionIdAsc(
            Long catalogItemId);
}
