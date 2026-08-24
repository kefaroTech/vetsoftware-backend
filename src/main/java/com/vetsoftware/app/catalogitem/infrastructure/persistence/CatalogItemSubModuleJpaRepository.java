package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CatalogItemSubModuleJpaRepository
        extends
            JpaRepository<CatalogItemSubModuleJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"catalogItem", "subModule"})
    List<CatalogItemSubModuleJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"catalogItem", "subModule"})
    Optional<CatalogItemSubModuleJpaEntity> findById(Long id);

    /**
     * El {@code @EntityGraph} es obligatorio aquí y no en las otras dos puentes: el
     * mapper sí lee {@code name} y {@code code} del submódulo para construir el
     * {@code SubModuleRef}, así que sin él cada fila del listado dispara su propio
     * SELECT.
     */
    @EntityGraph(attributePaths = {"catalogItem", "subModule"})
    List<CatalogItemSubModuleJpaEntity> findAllByCatalogItem_IdOrderByIdAsc(Long catalogItemId);

    /**
     * El {@code @SQLRestriction} de la entidad ya acota esto a las filas activas.
     */
    boolean existsByCatalogItem_Id(Long catalogItemId);

    /**
     * Ignora el borrado lógico: la fila dada de baja sigue ocupando
     * {@code uq_catalog_item_sub_modules} aunque la aplicación no la vea. Nativa
     * porque es la única forma de esquivar el {@code @SQLRestriction}.
     */
    @Query(value = """
            SELECT id FROM catalog_item_sub_modules
            WHERE catalog_item_id = :catalogItemId AND sub_module_id = :subModuleId
            """, nativeQuery = true)
    Optional<Long> findAnyIdByPair(@Param("catalogItemId") Long catalogItemId,
            @Param("subModuleId") Long subModuleId);

    @Query(value = """
            SELECT COUNT(*) FROM catalog_item_sub_modules
            WHERE catalog_item_id = :catalogItemId AND sub_module_id = :subModuleId
              AND enabled = TRUE
            """, nativeQuery = true)
    long countEnabledByPair(@Param("catalogItemId") Long catalogItemId,
            @Param("subModuleId") Long subModuleId);

    /**
     * Sin {@code version = version + 1} en el {@code SET} porque esta tabla no está
     * versionada: es puente y va exenta con {@code E2_TABLA_PUENTE}. La regla
     * {@code UPDATE_MASIVO_MUEVE_LA_VERSION} levanta el mapa tabla → versionada del
     * censo de {@code @Entity}, así que no la exige aquí.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE catalog_item_sub_modules SET enabled = TRUE WHERE id = :id", nativeQuery = true)
    int reactivate(@Param("id") Long id);
}
