package com.vetsoftware.app.paymentattempt.infrastructure.persistence;

import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * <strong>Sin ninguna {@code @Query} de {@code UPDATE} ni {@code DELETE}, y es
 * una decision.</strong> Lo unico que muta es {@code next_attempt_at}, y se
 * escribe por el ciclo leer-modificar-guardar de la entidad, que es donde
 * {@code @Version} <em>si</em> protege. Un {@code UPDATE} masivo iria directo a
 * la base sin comprobar ni incrementar nada, y el {@code save} concurrente que
 * llegara con la version vieja casaria igual y pisaria el cambio
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, #53).
 */
public interface PaymentAttemptJpaRepository extends JpaRepository<PaymentAttemptJpaEntity, Long> {

    Optional<PaymentAttemptJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    Page<PaymentAttemptJpaEntity> findAllByCompanyId(Long companyId, Pageable pageable);

    Page<PaymentAttemptJpaEntity> findAllByCompanyIdAndBillingDocumentId(Long companyId,
            Long billingDocumentId, Pageable pageable);

    /**
     * La cola de reintentos: <strong>sin filtro de empresa a proposito, y no es una
     * fuga</strong>. Es uno de los nueve barridos de plataforma y su indice
     * ({@code ix_payment_attempts_retry_queue}) va sobre {@code next_attempt_at}
     * sin la empresa delante justamente para esto. Lo que lo mantiene legal es que
     * el unico puerto de entrada que lo consume,
     * {@code ListDuePaymentAttemptsUseCase}, esta cerrado a
     * {@code hasRole('SYSTEM')} a secas ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}).
     */
    @Query("""
            select a from PaymentAttemptJpaEntity a
            where a.nextAttemptAt is not null and a.nextAttemptAt <= :dueBefore
            """)
    Page<PaymentAttemptJpaEntity> findAllDueForRetry(@Param("dueBefore") LocalDateTime dueBefore,
            Pageable pageable);

    /**
     * Ultimo consecutivo gastado sobre el documento; {@code null} si es el primer
     * intento. Se lee dentro de la transaccion que inserta, porque
     * {@code uq_payment_attempts_number} no admite dos iguales.
     */
    @Query("""
            select max(a.attemptNumber) from PaymentAttemptJpaEntity a
            where a.companyId = :companyId and a.billingDocumentId = :billingDocumentId
            """)
    Integer findMaxAttemptNumber(@Param("companyId") Long companyId,
            @Param("billingDocumentId") Long billingDocumentId);

    /**
     * Intentos imputables al cliente en la ventana.
     *
     * <p>
     * La clase excluida entra <strong>por parametro y no como literal de enum en el
     * JPQL</strong>: el literal obliga a escribir el nombre completamente
     * cualificado dentro de la consulta, que es una cadena que el compilador no
     * revisa y que un renombrado del paquete rompe en tiempo de ejecucion.
     */
    @Query("""
            select count(a) from PaymentAttemptJpaEntity a
            where a.companyId = :companyId
              and a.billingDocumentId = :billingDocumentId
              and a.declineKind <> :excludedKind
              and a.attemptedAt >= :since
            """)
    long countChargeableSince(@Param("companyId") Long companyId,
            @Param("billingDocumentId") Long billingDocumentId, @Param("since") LocalDateTime since,
            @Param("excludedKind") DeclineKind excludedKind);
}
