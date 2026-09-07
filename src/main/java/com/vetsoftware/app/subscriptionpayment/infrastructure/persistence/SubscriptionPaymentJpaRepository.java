package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Sin {@code @Query} de {@code UPDATE} ni {@code DELETE}: los importes de un
 * pago no mutan (R1) y los dos campos que si lo hacen -{@code status} y
 * {@code reconciled_at}- se escriben por el ciclo leer-modificar-guardar de la
 * entidad, donde {@code @Version} si protege.
 */
public interface SubscriptionPaymentJpaRepository
        extends
            JpaRepository<SubscriptionPaymentJpaEntity, Long> {

    Optional<SubscriptionPaymentJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * {@code SELECT ... FOR UPDATE} <strong>acotado por empresa</strong>.
     *
     * <p>
     * El bloqueo es lo que serializa el <em>read-then-write</em> de R3: se toma
     * antes de sumar lo ya aplicado, de modo que dos aplicaciones concurrentes no
     * puedan leer la misma suma y pasar las dos. Va acotado porque la variante
     * ancha concederia un bloqueo pesimista sobre la fila de otro tenant antes de
     * cualquier comprobacion: lo soltaria el rollback, pero se habria concedido.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from SubscriptionPaymentJpaEntity p
            where p.id = :id and p.companyId = :companyId
            """)
    Optional<SubscriptionPaymentJpaEntity> lockByIdAndCompanyId(@Param("id") Long id,
            @Param("companyId") Long companyId);

    Optional<SubscriptionPaymentJpaEntity> findByCompanyIdAndClientRequestId(Long companyId,
            String clientRequestId);

    /**
     * <strong>Sin filtro de empresa a proposito, y no es una fuga:</strong>
     * {@code uq_subscription_payments_gateway} es global, asi que la unica forma de
     * saber si una referencia de pasarela ya esta tomada es preguntar sin acotar.
     * Devuelve una fila como maximo -no es un listado- y el caso de uso comprueba
     * la empresa antes de exponer nada: si la referencia es de otra clinica,
     * rechaza sin revelar de quien.
     */
    Optional<SubscriptionPaymentJpaEntity> findByGatewayAndGatewayReference(String gateway,
            String gatewayReference);

    Page<SubscriptionPaymentJpaEntity> findAllByCompanyId(Long companyId, Pageable pageable);

    /**
     * Pagos PENDING de pasarela envejecidos. {@code gateway is not null} los
     * distingue de un pago manual por transferencia a la espera de conciliacion,
     * que no es lo que esta metrica vigila.
     */
    long countByStatusAndGatewayIsNotNullAndReceivedAtBefore(SubscriptionPaymentStatus status,
            LocalDateTime threshold);

    /**
     * Las mismas filas que cuenta
     * {@link #countByStatusAndGatewayIsNotNullAndReceivedAtBefore}, pero devueltas
     * y en orden ascendente: el barrido de conciliación de {@code paymentgateway}
     * necesita las más viejas primero, no un conteo.
     */
    List<SubscriptionPaymentJpaEntity> findByStatusAndGatewayIsNotNullAndReceivedAtBeforeOrderByReceivedAtAsc(
            SubscriptionPaymentStatus status, LocalDateTime threshold, Pageable pageable);

    /**
     * El orden lo añade el {@code Sort} del {@code Pageable}.
     */
    @Query("""
            SELECT p FROM SubscriptionPaymentJpaEntity p
            WHERE (:companyId IS NULL OR p.companyId = :companyId)
              AND (:status IS NULL OR p.status = :status)
              AND (:receivedFrom IS NULL OR p.receivedAt >= :receivedFrom)
              AND (:receivedTo IS NULL OR p.receivedAt <= :receivedTo)
              AND (:pendingThreshold IS NULL
                   OR (p.gateway IS NOT NULL AND p.receivedAt < :pendingThreshold))
            """)
    Page<SubscriptionPaymentJpaEntity> findAllFiltered(@Param("companyId") Long companyId,
            @Param("status") SubscriptionPaymentStatus status,
            @Param("receivedFrom") LocalDateTime receivedFrom,
            @Param("receivedTo") LocalDateTime receivedTo,
            @Param("pendingThreshold") LocalDateTime pendingThreshold, Pageable pageable);

    @Query("""
            SELECT p FROM SubscriptionPaymentJpaEntity p
            WHERE (:companyId IS NULL OR p.companyId = :companyId)
              AND (:status IS NULL OR p.status = :status)
              AND (:receivedFrom IS NULL OR p.receivedAt >= :receivedFrom)
              AND (:receivedTo IS NULL OR p.receivedAt <= :receivedTo)
              AND (:pendingThreshold IS NULL
                   OR (p.gateway IS NOT NULL AND p.receivedAt < :pendingThreshold))
            ORDER BY p.receivedAt DESC, p.id DESC
            """)
    List<SubscriptionPaymentJpaEntity> findAllFilteredForExport(@Param("companyId") Long companyId,
            @Param("status") SubscriptionPaymentStatus status,
            @Param("receivedFrom") LocalDateTime receivedFrom,
            @Param("receivedTo") LocalDateTime receivedTo,
            @Param("pendingThreshold") LocalDateTime pendingThreshold);
}
