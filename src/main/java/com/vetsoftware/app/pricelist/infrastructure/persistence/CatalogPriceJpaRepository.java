package com.vetsoftware.app.pricelist.infrastructure.persistence;

import com.vetsoftware.app.pricelist.domain.BillingCycle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las consultas derivadas del nombre cuelgan todas de
 * {@code uq_catalog_prices_tier (price_list_id, catalog_item_id, billing_cycle,
 * tier_min)}, que sirve de indice de busqueda con sus igualdades por delante.
 *
 * <p>
 * Las tres {@code @Query} nativas del final son la excepcion, y existen por una
 * sola razon: son las unicas que pueden ver las filas dadas de baja. El
 * {@code @SQLRestriction} de la entidad las esconde, pero
 * {@code uq_catalog_prices_tier} no las ignora.
 */
public interface CatalogPriceJpaRepository extends JpaRepository<CatalogPriceJpaEntity, Long> {

    Page<CatalogPriceJpaEntity> findAllByPriceListId(Long priceListId, Pageable pageable);

    /**
     * Los hermanos de tramo del candidato. {@code @SQLRestriction} ya descarta los
     * deshabilitados, que es lo correcto: un tramo dado de baja no compite por
     * ninguna unidad.
     */
    List<CatalogPriceJpaEntity> findAllByPriceListIdAndCatalogItemIdAndBillingCycle(
            Long priceListId, Long catalogItemId, BillingCycle billingCycle);

    /**
     * Todos los tramos activos de la lista, en el orden en que se examina la
     * cobertura. Cuelga del mismo {@code uq_catalog_prices_tier} que el resto, asi
     * que la ordenacion sale del indice.
     */
    List<CatalogPriceJpaEntity> findAllByPriceListIdOrderByCatalogItemIdAscBillingCycleAscTierMinAsc(
            Long priceListId);

    long countByPriceListId(Long priceListId);

    /**
     * El id del precio que ocupa exactamente ese tramo, <strong>ignorando el
     * borrado logico</strong>. Quitar un tramo de una lista en borrador y volver a
     * ponerlo con el mismo {@code tier_min} es una operacion normal armando una
     * tarifa; sin esto choca contra una fila retirada que nadie puede ver y
     * devuelve un 409 opaco sobre dinero.
     */
    @Query(value = """
            SELECT id FROM catalog_prices
            WHERE price_list_id = :priceListId AND catalog_item_id = :catalogItemId
              AND billing_cycle = :billingCycle AND tier_min = :tierMin
            """, nativeQuery = true)
    Optional<Long> findAnyIdByTier(@Param("priceListId") Long priceListId,
            @Param("catalogItemId") Long catalogItemId, @Param("billingCycle") String billingCycle,
            @Param("tierMin") int tierMin);

    /** {@code long} y no {@code boolean}: literal booleano proyectado, #196. */
    @Query(value = """
            SELECT COUNT(*) FROM catalog_prices
            WHERE price_list_id = :priceListId AND catalog_item_id = :catalogItemId
              AND billing_cycle = :billingCycle AND tier_min = :tierMin AND enabled = TRUE
            """, nativeQuery = true)
    long countEnabledByTier(@Param("priceListId") Long priceListId,
            @Param("catalogItemId") Long catalogItemId, @Param("billingCycle") String billingCycle,
            @Param("tierMin") int tierMin);

    /**
     * Deshace la baja logica. El {@code SET} mueve tambien {@code version}
     * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, #53): sin eso, un {@code save}
     * cargado antes de la reactivacion reescribe {@code enabled} con su valor viejo
     * y su {@code WHERE version = ?} casa igual, con lo que una edicion concurrente
     * vuelve a apagar en silencio lo que la reactivacion acababa de encender.
     *
     * <p>
     * {@code version} NO va en el {@code WHERE}: reactivar es deliberado y debe
     * ejecutarse siempre, no competir con una edicion.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE catalog_prices
            SET enabled = true, version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@Param("id") Long id);
}
