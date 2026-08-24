package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import com.vetsoftware.app.catalogitem.domain.RelationType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CatalogItemDependencyJpaRepository
        extends
            JpaRepository<CatalogItemDependencyJpaEntity, Long> {

    @EntityGraph(attributePaths = {"catalogItem", "relatedItem"})
    List<CatalogItemDependencyJpaEntity> findAllByCatalogItem_IdOrderByIdAsc(Long catalogItemId);

    boolean existsByCatalogItem_IdOrRelatedItem_Id(Long catalogItemId, Long relatedItemId);

    /**
     * Los arcos del grafo de la regla R16, proyectados a dos columnas.
     *
     * <p>
     * {@code d.catalogItem.id} sobre un {@code @ManyToOne} lee la columna de la
     * clave foránea y <strong>no genera JOIN</strong>: el grafo se carga con una
     * sola consulta de dos enteros por fila, sin hidratar ningún artículo. El
     * {@code @SQLRestriction} de la entidad ya deja fuera los arcos desactivados,
     * que es el mismo criterio que usa la consulta de vigilancia de R16.
     */
    @Query("""
            SELECT d.catalogItem.id, d.relatedItem.id
            FROM CatalogItemDependencyJpaEntity d
            WHERE d.relationType = :relationType
            """)
    List<Object[]> findEdgesByRelationType(@Param("relationType") RelationType relationType);

    /** Ignora el borrado lógico: la terna única no lo ignora. */
    @Query(value = """
            SELECT id FROM catalog_item_dependencies
            WHERE catalog_item_id = :catalogItemId AND related_item_id = :relatedItemId
              AND relation_type = :relationType
            """, nativeQuery = true)
    Optional<Long> findAnyIdByTriple(@Param("catalogItemId") Long catalogItemId,
            @Param("relatedItemId") Long relatedItemId, @Param("relationType") String relationType);

    @Query(value = """
            SELECT COUNT(*) FROM catalog_item_dependencies
            WHERE catalog_item_id = :catalogItemId AND related_item_id = :relatedItemId
              AND relation_type = :relationType AND enabled = TRUE
            """, nativeQuery = true)
    long countEnabledByTriple(@Param("catalogItemId") Long catalogItemId,
            @Param("relatedItemId") Long relatedItemId, @Param("relationType") String relationType);

    /** Tabla puente sin {@code @Version} ({@code E2_TABLA_PUENTE}): sin bump. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE catalog_item_dependencies SET enabled = TRUE WHERE id = :id", nativeQuery = true)
    int reactivate(@Param("id") Long id);
}
