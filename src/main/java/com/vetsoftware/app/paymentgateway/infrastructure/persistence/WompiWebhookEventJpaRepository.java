package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WompiWebhookEventJpaRepository
        extends
            JpaRepository<WompiWebhookEventJpaEntity, Long> {

    boolean existsByGatewayAndEventChecksum(String gateway, String eventChecksum);

    /**
     * {@code gateway_webhook_events} está versionada: el {@code UPDATE} masivo
     * mueve {@code version} en el mismo {@code SET}, porque el ciclo
     * leer-modificar-guardar de {@code @Version} no protege esta escritura de
     * conjunto ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}).
     */
    @Modifying
    @Query("UPDATE WompiWebhookEventJpaEntity e SET e.rawBody = NULL, e.version = e.version + 1"
            + " WHERE e.receivedAt < :cutoff AND e.rawBody IS NOT NULL")
    int purgeRawBodyReceivedBefore(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Sin JPQL cruzado en el {@code FROM}: la subconsulta evita duplicar filas y no
     * requiere una asociación JPA entre las dos features; ver el javadoc de
     * {@link WompiWebhookEventJpaEntity}.
     */
    @Query("""
            SELECT w FROM WompiWebhookEventJpaEntity w
            WHERE (:reference IS NULL OR w.gatewayReference = :reference)
              AND (:from IS NULL OR w.receivedAt >= :from)
              AND (:to IS NULL OR w.receivedAt <= :to)
              AND (:companyId IS NULL OR EXISTS (
                    SELECT 1 FROM SubscriptionPaymentJpaEntity p
                    WHERE p.gateway = w.gateway AND p.gatewayReference = w.gatewayReference
                      AND p.companyId = :companyId))
            """)
    Page<WompiWebhookEventJpaEntity> search(@Param("reference") String reference,
            @Param("companyId") Long companyId, @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to, Pageable pageable);
}
