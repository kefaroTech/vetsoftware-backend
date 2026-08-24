package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BundleComponentJpaRepository
        extends
            JpaRepository<BundleComponentJpaEntity, Long> {

    @EntityGraph(attributePaths = {"bundleItem", "componentItem"})
    List<BundleComponentJpaEntity> findAllByBundleItem_IdOrderByIdAsc(Long bundleItemId);

    boolean existsByBundleItem_IdOrComponentItem_Id(Long bundleItemId, Long componentItemId);

    /** Ignora el borrado lógico: {@code uq_bundle_components} tampoco lo ignora. */
    @Query(value = """
            SELECT id FROM bundle_components
            WHERE bundle_item_id = :bundleItemId AND component_item_id = :componentItemId
            """, nativeQuery = true)
    Optional<Long> findAnyIdByPair(@Param("bundleItemId") Long bundleItemId,
            @Param("componentItemId") Long componentItemId);

    @Query(value = """
            SELECT COUNT(*) FROM bundle_components
            WHERE bundle_item_id = :bundleItemId AND component_item_id = :componentItemId
              AND enabled = TRUE
            """, nativeQuery = true)
    long countEnabledByPair(@Param("bundleItemId") Long bundleItemId,
            @Param("componentItemId") Long componentItemId);

    /** Tabla puente sin {@code @Version} ({@code E2_TABLA_PUENTE}): sin bump. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE bundle_components SET enabled = TRUE WHERE id = :id", nativeQuery = true)
    int reactivate(@Param("id") Long id);
}
