package com.vetsoftware.app.subscriptionitemlimit.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Consultas de los techos congelados.
 *
 * <p>
 * <strong>Ni una consulta de escritura masiva.</strong> La propagación de
 * mejoras carga las filas, las mueve por el agregado y las guarda una a una: es
 * deliberado. Un {@code UPDATE} de conjunto no sabría distinguir una mejora de
 * un recorte —esa decisión vive en el dominio— y, además, sobre una tabla
 * versionada tendría que arrastrar la versión a mano para no dejar la puerta
 * abierta a que un guardado concurrente lo deshiciera sin ruido.
 */
public interface SubscriptionItemLimitJpaRepository
        extends
            JpaRepository<SubscriptionItemLimitJpaEntity, Long> {

    Optional<SubscriptionItemLimitJpaEntity> findByCompanyIdAndSubscriptionItemIdAndLimitDimensionId(
            Long companyId, Long subscriptionItemId, Long limitDimensionId);

    List<SubscriptionItemLimitJpaEntity> findAllByCompanyIdOrderByLimitDimensionIdAscIdAsc(
            Long companyId);

    /**
     * Los techos de las líneas <em>vivas</em> de un artículo sobre un eje, en todas
     * las empresas: la consulta de la propagación de mejoras.
     *
     * <p>
     * «Viva» es {@code effectiveTo IS NULL}. Sin ese filtro, la mejora alcanzaría
     * también a líneas ya cerradas y reescribiría el techo de contratos que
     * terminaron —falseando el histórico que la tabla existe para conservar—.
     */
    @Query("""
            SELECT l FROM SubscriptionItemLimitJpaEntity l
            WHERE l.limitDimensionId = :limitDimensionId
              AND l.subscriptionItemId IN (
                  SELECT i.id FROM SubscriptionItemJpaEntity i
                  WHERE i.catalogItemId = :catalogItemId AND i.effectiveTo IS NULL)
            ORDER BY l.companyId ASC, l.id ASC
            """)
    List<SubscriptionItemLimitJpaEntity> findAllLiveByCatalogItemIdAndLimitDimensionId(
            @Param("catalogItemId") Long catalogItemId,
            @Param("limitDimensionId") Long limitDimensionId);
}
