package com.vetsoftware.app.companylimitoverride.infrastructure.persistence;

import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionItemJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Los candidatos a techo que no viven en esta rodaja, en tres consultas de solo
 * lectura.
 *
 * <p>
 * Sigue el patrón que ya usa {@code ContractItemJpaRepository} en
 * {@code entitlement}: un {@code Repository} de solo lectura sobre la entidad
 * de otra feature, dentro de {@code infrastructure/persistence}, que es el
 * único sitio donde el {@code CLAUDE.md} permite el cruce.
 *
 * <p>
 * <strong>Las tres llevan la empresa en el {@code WHERE}, sin
 * excepción.</strong> Un techo congelado sin ese filtro es la cifra que negoció
 * cada uno de los quinientos tenants sobre ese eje, y esta clase no declara ni
 * una consulta que pueda devolver filas de más de una empresa.
 *
 * <p>
 * <strong>Y ninguna escribe.</strong> El techo efectivo se resuelve, no se
 * guarda: quien guarda el resultado junto al contador es el recálculo de
 * permisos.
 */
public interface EffectiveLimitCandidateJpaRepository
        extends
            Repository<SubscriptionItemJpaEntity, Long> {

    /**
     * Los techos congelados de las líneas <strong>vivas</strong> del contrato sobre
     * un eje.
     *
     * <p>
     * «Viva» es {@code effective_to IS NULL}. Sin ese filtro entrarían los techos
     * de líneas ya terminadas y una clínica que bajó de plan seguiría leyendo el
     * cupo del plan que dejó: el histórico que la tabla existe para conservar se
     * convertiría en el techo vigente.
     *
     * <p>
     * {@code enabled = TRUE} va explícito en las dos tablas porque una consulta
     * nativa no pasa por el {@code @SQLRestriction} de las entidades.
     */
    @Query(value = """
            SELECT sil.mode AS mode, sil.limit_quantity AS limitQuantity
            FROM subscription_item_limits sil
            JOIN subscription_items i ON i.id = sil.subscription_item_id
                 AND i.company_id = sil.company_id AND i.enabled = TRUE
                 AND i.effective_to IS NULL
            WHERE sil.company_id = :companyId
              AND sil.limit_dimension_id = :limitDimensionId
              AND sil.enabled = TRUE
            ORDER BY sil.id ASC
            """, nativeQuery = true)
    List<LimitCeilingView> findContractedCeilings(@Param("companyId") Long companyId,
            @Param("limitDimensionId") Long limitDimensionId);

    /**
     * Los escalones de fábrica de los artículos que la empresa usa
     * <strong>gratis</strong> sobre un eje.
     *
     * <p>
     * «Gratis» es {@code charge_mode = 'FREE_LIMITED'} en la línea del contrato, y
     * no la ausencia de precio: la línea existe y tiene su artículo, lo que pasa es
     * que no se cobra. Es el escalón que queda cuando una prueba vence con
     * desenlace limitado, y su techo no está congelado en ninguna parte —por eso
     * hay que ir al catálogo a buscarlo—.
     *
     * <p>
     * Las líneas {@code EXPIRED_READ_ONLY} quedan fuera a propósito: ahí el cliente
     * consulta lo que ya cargó y no crea nada, así que no aportan un techo bajo el
     * que crear.
     */
    @Query(value = """
            SELECT cil.mode AS mode, cil.limit_quantity AS limitQuantity
            FROM subscription_items i
            JOIN catalog_item_limits cil ON cil.catalog_item_id = i.catalog_item_id
                 AND cil.limit_dimension_id = :limitDimensionId AND cil.enabled = TRUE
            WHERE i.company_id = :companyId
              AND i.enabled = TRUE
              AND i.effective_to IS NULL
              AND i.charge_mode = 'FREE_LIMITED'
            ORDER BY cil.id ASC
            """, nativeQuery = true)
    List<LimitCeilingView> findFreeTierCeilings(@Param("companyId") Long companyId,
            @Param("limitDimensionId") Long limitDimensionId);

    /**
     * Cuántos contratos de la empresa se firmaron cuando el eje <em>ya existía</em>
     * (D-74).
     *
     * <p>
     * Devuelve un recuento y no un booleano porque la respuesta útil es «alguno», y
     * eso se lee mejor que un {@code COUNT(*) > 0} que MySQL entrega como 0/1.
     * Cualquier valor mayor que cero significa que la ausencia de techo <b>sí</b>
     * es techo cero para esta empresa.
     *
     * <p>
     * <strong>El criterio es «algún contrato», no «el último»</strong>, y esa
     * elección tiene consecuencias: es la conservadora. Un eje que nació entre dos
     * contratos de la misma clínica queda tratado como preexistente, que es techo
     * cero y no barra libre. Al revés —dar por bueno «sin techo» porque el contrato
     * más reciente es anterior al eje— se regalaría cupo sin que nadie lo hubiera
     * vendido.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM subscriptions s
            JOIN limit_dimensions ld ON ld.id = :limitDimensionId AND ld.enabled = TRUE
            WHERE s.company_id = :companyId
              AND s.enabled = TRUE
              AND ld.available_from <= s.start_date
            """, nativeQuery = true)
    long countContractsSignedAfterAxis(@Param("companyId") Long companyId,
            @Param("limitDimensionId") Long limitDimensionId);
}
