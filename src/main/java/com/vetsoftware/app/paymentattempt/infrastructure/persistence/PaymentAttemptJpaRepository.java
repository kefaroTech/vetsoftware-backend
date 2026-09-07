package com.vetsoftware.app.paymentattempt.infrastructure.persistence;

import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import java.time.LocalDateTime;
import java.util.List;
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
     *
     * <p>
     * <strong>Solo el ultimo intento de cada documento, nunca uno
     * superado.</strong> Un documento con dos filas -la vieja, vencida, y una nueva
     * que ya la sustituyo con su propio {@code next_attempt_at}- volveria a salir
     * por la vieja para siempre si no se filtrara: su fecha ya paso y nada la
     * mueve. El subselect exige que el numero de intento sea el maximo del
     * documento. Y solo mientras el documento siga debiendo: un pago manual o una
     * nota credito que lo salden dejarian la fila vencida saliendo cada dia sin
     * nada que cobrar.
     *
     * <p>
     * <strong>Ni un documento anulado.</strong> {@code voidDocument()} no exige
     * saldo cero -solo que la factura externa no exista todavia-, asi que un
     * documento {@code VOIDED} con aplicaciones previas puede seguir teniendo
     * {@code balance_amount > 0} y, sin este filtro, la escalera lo seguiria
     * cobrando. {@code 'VOIDED'} va literal y no como {@code IssueStatus.VOIDED}:
     * ese enum es de {@code subscriptionbilling} y el vertical slicing no deja
     * importar dominio ajeno aqui.
     *
     * <p>
     * <strong>Ni de un contrato ya cerrado.</strong> Sustituir o cancelar un
     * contrato no anula por si solo sus documentos pendientes; sin este filtro la
     * escalera seguiria cobrando el documento de una suscripcion {@code CANCELLED}
     * o {@code EXPIRED}. Los literales de estado son los mismos que
     * {@code SubscriptionStatus.CURRENT}, repetidos aqui por el mismo motivo que
     * {@code 'VOIDED'}.
     *
     * <p>
     * <strong>Ni con un pago {@code PENDING} o {@code REFUNDED} aplicado.</strong>
     * Espejo del {@code NOT EXISTS} de {@code NewRecurringChargeJpaRepository}: un
     * {@code PENDING} sin resolver ya lo bloquea {@code PendingPaymentQueryPort} en
     * el cobro nuevo, pero sin este filtro la escalera de reintentos lo volveria a
     * ofrecer igual; y un {@code REFUNDED} -devolucion de cortesia- no debe
     * reactivar un cobro que ya se decidio no cobrar.
     */
    @Query("""
            select a from PaymentAttemptJpaEntity a
            where a.nextAttemptAt is not null and a.nextAttemptAt <= :dueBefore
              and a.attemptNumber = (
                  select max(a2.attemptNumber) from PaymentAttemptJpaEntity a2
                  where a2.companyId = a.companyId and a2.billingDocumentId = a.billingDocumentId
              )
              and exists (
                  select d.id from SubscriptionBillingDocumentJpaEntity d
                  where d.id = a.billingDocumentId and d.companyId = a.companyId
                    and d.balanceAmount > 0 and d.issueStatus <> 'VOIDED'
                    and exists (
                        select s.id from SubscriptionJpaEntity s
                        where s.id = d.subscriptionId
                          and s.status in ('TRIALING', 'ACTIVE', 'PAST_DUE', 'READ_ONLY')
                    )
              )
              and not exists (
                  select bda.id from BillingDocumentApplicationJpaEntity bda
                  where bda.targetDocument.id = a.billingDocumentId
                    and bda.sourceKind = com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind.PAYMENT
                    and bda.payment.status in (
                        com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus.PENDING,
                        com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus.REFUNDED)
              )
            """)
    Page<PaymentAttemptJpaEntity> findAllDueForRetry(@Param("dueBefore") LocalDateTime dueBefore,
            Pageable pageable);

    /**
     * Ultimo intento de cada documento con saldo de la empresa, cuando ese ultimo
     * intento es de la clase dada. Espejo del subselect de
     * {@link #findAllDueForRetry}, pero sin filtrar por {@code next_attempt_at}:
     * aqui el vacio es precisamente lo que hay que reactivar tras fijar una tarjeta
     * nueva como predeterminada.
     *
     * <p>
     * <strong>Y con el mismo filtro de suscripcion vigente.</strong> Sin el, fijar
     * una tarjeta nueva reprograma a "ahora" los intentos de un documento cuyo
     * contrato ya esta {@code CANCELLED} o {@code EXPIRED}: no llega a cobrarse
     * porque {@link #findAllDueForRetry} si filtra, pero queda reprogramado a
     * perpetuidad.
     */
    @Query("""
            select a from PaymentAttemptJpaEntity a
            where a.companyId = :companyId and a.declineKind = :declineKind
              and a.attemptNumber = (
                  select max(a2.attemptNumber) from PaymentAttemptJpaEntity a2
                  where a2.companyId = a.companyId and a2.billingDocumentId = a.billingDocumentId
              )
              and exists (
                  select d.id from SubscriptionBillingDocumentJpaEntity d
                  where d.id = a.billingDocumentId and d.companyId = a.companyId
                    and d.balanceAmount > 0 and d.issueStatus <> 'VOIDED'
                    and exists (
                        select s.id from SubscriptionJpaEntity s
                        where s.id = d.subscriptionId
                          and s.status in ('TRIALING', 'ACTIVE', 'PAST_DUE', 'READ_ONLY')
                    )
              )
            """)
    List<PaymentAttemptJpaEntity> findLastAttemptsByCompanyIdAndDeclineKind(
            @Param("companyId") Long companyId, @Param("declineKind") DeclineKind declineKind);

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

    /**
     * Tamano de la cola de reintentos: intentos con un proximo reintento ya
     * programado. Sin el filtro de "ultimo intento por documento" de
     * {@link #findAllDueForRetry}: aqui la pregunta es cuanto hay en vuelo, no que
     * hay que cobrar ahora mismo.
     */
    long countByNextAttemptAtGreaterThan(LocalDateTime now);
}
