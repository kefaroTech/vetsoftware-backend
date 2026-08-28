package com.vetsoftware.app.paymentreversal.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Sin ninguna {@code @Query} de {@code UPDATE} ni de {@code DELETE}, y no es
 * casualidad: los tres campos que mutan —acuse, oposicion y desenlace— se
 * escriben por el ciclo leer-modificar-guardar de la entidad, que es el unico
 * camino donde {@code @Version} protege de verdad. Una escritura masiva por
 * {@code @Query} va directa a la base sin comprobar ni incrementar la version,
 * y el {@code save} concurrente que llegue con la version vieja pisaria el
 * cambio sin excepcion y sin log ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, #53).
 */
public interface PaymentReversalRequestJpaRepository
        extends
            JpaRepository<PaymentReversalRequestJpaEntity, Long> {

    Optional<PaymentReversalRequestJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    Optional<PaymentReversalRequestJpaEntity> findByCompanyIdAndPaymentId(Long companyId,
            Long paymentId);

    Page<PaymentReversalRequestJpaEntity> findAllByCompanyId(Long companyId, Pageable pageable);

    /**
     * <strong>Sin filtro de empresa a proposito.</strong> Es el barrido de
     * plataforma que encuentra los expedientes que vencen sin resolver, y lo sirve
     * {@code ix_payment_reversal_requests_deadline}, que va sobre
     * {@code (deadline_at, outcome)} <em>sin la empresa delante</em>: ponersela lo
     * haria inutil, porque la pregunta es que le vence a alguien, no que le vence a
     * esta clinica.
     *
     * <p>
     * El unico puerto que lo consume esta cerrado a {@code hasRole('SYSTEM')} a
     * secas. Declararlo aqui NO exime al caso de uso de la regla de aislamiento:
     * esa regla recorre codigo, no comentarios.
     *
     * <p>
     * El {@code outcome is null} va en la consulta y no en memoria: filtrar despues
     * de paginar devolveria paginas mayormente vacias segun envejece la tabla y el
     * total del {@code Page} mentiria.
     */
    @Query("""
            select r from PaymentReversalRequestJpaEntity r
            where r.outcome is null and r.deadlineAt < :before
            """)
    Page<PaymentReversalRequestJpaEntity> findAllExpiringBefore(
            @Param("before") LocalDateTime before, Pageable pageable);
}
