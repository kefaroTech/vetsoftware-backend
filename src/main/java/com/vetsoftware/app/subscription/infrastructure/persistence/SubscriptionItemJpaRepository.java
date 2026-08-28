package com.vetsoftware.app.subscription.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionItemJpaRepository
        extends
            JpaRepository<SubscriptionItemJpaEntity, Long> {

    @EntityGraph(attributePaths = {"company", "subscription"})
    Optional<SubscriptionItemJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"company", "subscription"})
    Optional<SubscriptionItemJpaEntity> findByCreatedAmendmentIdAndCompany_Id(Long amendmentId,
            Long companyId);

    @EntityGraph(attributePaths = {"company", "subscription"})
    List<SubscriptionItemJpaEntity> findAllByCreatedAmendmentIdAndCompany_IdOrderByTierMinAsc(
            Long amendmentId, Long companyId);

    @EntityGraph(attributePaths = {"company", "subscription"})
    Page<SubscriptionItemJpaEntity> findAllBySubscription_IdAndCompany_Id(Long subscriptionId,
            Long companyId, Pageable pageable);

    /**
     * La linea abierta de ese articulo. Como maximo una: lo garantiza
     * {@code uq_subscription_items_current} sobre la columna generada.
     */
    @EntityGraph(attributePaths = {"company", "subscription"})
    Optional<SubscriptionItemJpaEntity> findByCompany_IdAndSubscription_IdAndCatalogItemIdAndEffectiveToIsNull(
            Long companyId, Long subscriptionId, Long catalogItemId);

    /**
     * Lo que estaba contratado ese dia. <strong>Este es el criterio de
     * «vigente»</strong> escrito en SQL, y es el mismo que
     * {@code EffectivePeriod.isCurrentOn}: ya empezo y todavia no ha terminado.
     * Igualdades primero y rango despues, para que use
     * {@code ix_subscription_items_vigencia}.
     */
    @EntityGraph(attributePaths = {"company", "subscription"})
    @Query("""
            SELECT i
            FROM SubscriptionItemJpaEntity i
            WHERE i.company.id = :companyId
              AND i.subscription.id = :subscriptionId
              AND i.effectiveFrom <= :day
              AND (i.effectiveTo IS NULL OR i.effectiveTo > :day)
            """)
    Page<SubscriptionItemJpaEntity> findCurrentOn(@Param("companyId") Long companyId,
            @Param("subscriptionId") Long subscriptionId, @Param("day") LocalDate day,
            Pageable pageable);

    /**
     * El mismo criterio de vigencia sin paginar, acotado por empresa y por
     * contrato. Lo consume el prorrateo de la cancelacion, que necesita sumar todas
     * las lineas y no una pagina de ellas.
     */
    @EntityGraph(attributePaths = {"company", "subscription"})
    @Query("""
            SELECT i
            FROM SubscriptionItemJpaEntity i
            WHERE i.company.id = :companyId
              AND i.subscription.id = :subscriptionId
              AND i.effectiveFrom <= :day
              AND (i.effectiveTo IS NULL OR i.effectiveTo > :day)
            ORDER BY i.effectiveFrom ASC, i.id ASC
            """)
    List<SubscriptionItemJpaEntity> findAllCurrentOn(@Param("companyId") Long companyId,
            @Param("subscriptionId") Long subscriptionId, @Param("day") LocalDate day);

    /**
     * Los tramos del mismo articulo que se pisarian con {@code [from, to)}.
     * Intervalo semiabierto: la linea que cierra el 30 y la que abre el 30 no se
     * solapan.
     *
     * <p>
     * {@code :excludeId} deja fuera la linea que se este editando; con {@code null}
     * no excluye ninguna, que es el caso del alta.
     */
    @EntityGraph(attributePaths = {"company", "subscription"})
    @Query("""
            SELECT i
            FROM SubscriptionItemJpaEntity i
            WHERE i.company.id = :companyId
              AND i.subscription.id = :subscriptionId
              AND i.catalogItemId = :catalogItemId
              AND (:excludeId IS NULL OR i.id <> :excludeId)
              AND i.effectiveFrom < COALESCE(:to, :openEnded)
              AND COALESCE(i.effectiveTo, :openEnded) > :from
            """)
    List<SubscriptionItemJpaEntity> findOverlapping(@Param("companyId") Long companyId,
            @Param("subscriptionId") Long subscriptionId,
            @Param("catalogItemId") Long catalogItemId, @Param("from") LocalDate from,
            @Param("to") LocalDate to, @Param("excludeId") Long excludeId,
            @Param("openEnded") LocalDate openEnded);

    /**
     * La consulta de vigilancia R7: los solapes <strong>ya ocurridos</strong>, en
     * toda la plataforma. Es nativa porque es un auto-{@code JOIN} y porque tiene
     * que ver {@code enabled} explicitamente: {@code @SQLRestriction} no aplica al
     * SQL nativo.
     *
     * <p>
     * {@code b.id > a.id} evita que cada par salga dos veces y que una fila se
     * compare consigo misma. <strong>Cero filas = sano.</strong>
     */
    @Query(value = """
            SELECT a.company_id       AS companyId,
                   a.subscription_id  AS subscriptionId,
                   a.catalog_item_id  AS catalogItemId,
                   a.item_code        AS itemCode,
                   a.id               AS firstItemId,
                   a.effective_from   AS firstFrom,
                   a.effective_to     AS firstTo,
                   b.id               AS secondItemId,
                   b.effective_from   AS secondFrom,
                   b.effective_to     AS secondTo
              FROM subscription_items a
              JOIN subscription_items b
                   ON  b.company_id      = a.company_id
                   AND b.subscription_id = a.subscription_id
                   AND b.catalog_item_id = a.catalog_item_id
                   AND b.id              > a.id
             WHERE a.enabled = TRUE
               AND b.enabled = TRUE
               AND a.effective_from < COALESCE(b.effective_to, '9999-12-31')
               AND b.effective_from < COALESCE(a.effective_to, '9999-12-31')
             ORDER BY a.company_id, a.subscription_id, a.catalog_item_id
            """, nativeQuery = true)
    List<OverlapProjection> findAllOverlaps();

    /** Proyeccion de {@link #findAllOverlaps()}. */
    interface OverlapProjection {
        Long getCompanyId();

        Long getSubscriptionId();

        Long getCatalogItemId();

        String getItemCode();

        Long getFirstItemId();

        LocalDate getFirstFrom();

        LocalDate getFirstTo();

        Long getSecondItemId();

        LocalDate getSecondFrom();

        LocalDate getSecondTo();
    }
}
