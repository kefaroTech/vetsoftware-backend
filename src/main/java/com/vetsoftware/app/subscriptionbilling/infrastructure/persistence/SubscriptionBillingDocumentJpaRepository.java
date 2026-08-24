package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * <b>Sin ningún {@code UPDATE} ni {@code DELETE} por {@code @Query}, y es una
 * decisión.</b> Todo lo que muta un documento —el estado, la referencia
 * externa, lo saldado— pasa por la entidad gestionada, donde {@code @Version}
 * compara la versión en el {@code WHERE} y la incrementa en el {@code SET}. Una
 * {@code @Query} de {@code UPDATE} va directa a la base: ni comprueba ni
 * incrementa nada, y el {@code save} concurrente que llegue con la versión
 * vieja casa igual y pisa el cambio, sin excepción, sin log y sin 409. Sobre
 * {@code settled_amount} —la columna de la que depende la mora— ese fallo
 * silencioso es exactamente el que suspende a quien ya pagó.
 */
public interface SubscriptionBillingDocumentJpaRepository
        extends
            JpaRepository<SubscriptionBillingDocumentJpaEntity, Long> {

    Optional<SubscriptionBillingDocumentJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Lectura de bloqueo sobre la fila del documento, acotada por empresa.
     *
     * <p>
     * Nativa para poder usar {@code FOR UPDATE}. Es lo que serializa el
     * <i>read-then-write</i> de todo lo que mueve {@code settled_amount}: sin él,
     * dos aplicaciones concurrentes leen la misma suma y las dos pasan. Si el
     * documento es de otro tenant no devuelve fila y no bloquea nada.
     */
    @Query(value = """
            SELECT *
            FROM subscription_billing_documents
            WHERE id = :id
              AND company_id = :companyId
            FOR UPDATE
            """, nativeQuery = true)
    Optional<SubscriptionBillingDocumentJpaEntity> lockByIdAndCompanyId(@Param("id") Long id,
            @Param("companyId") Long companyId);

    Page<SubscriptionBillingDocumentJpaEntity> findAllByCompanyId(Long companyId,
            Pageable pageable);

    /**
     * La lista de trabajo mensual. Sirve {@code ix_sbd_awaiting}
     * {@code (issue_status, created_date)} — barrido de plataforma, sin tenant
     * delante, y por eso solo lo alcanza un caso de uso cerrado a SYSTEM.
     */
    @Query(value = """
            SELECT *
            FROM subscription_billing_documents
            WHERE issue_status = 'AWAITING_EXTERNAL'
            """, countQuery = """
            SELECT COUNT(*)
            FROM subscription_billing_documents
            WHERE issue_status = 'AWAITING_EXTERNAL'
            """, nativeQuery = true)
    Page<SubscriptionBillingDocumentJpaEntity> findAllAwaitingExternal(Pageable pageable);

    /**
     * El barrido de mora, sobre {@code ix_sbd_overdue}
     * {@code (overdue_marker, due_date)}.
     *
     * <p>
     * <b>El marcador codifica «registrada, factura y no saldada»; el «vencida» lo
     * pone este {@code WHERE}.</b> No puede estar dentro de la columna generada
     * porque su expresión tiene que ser determinista y {@code CURRENT_DATE} no lo
     * es. Y {@code :today} llega por parámetro en vez de escribir
     * {@code CURRENT_DATE} aquí para que el caso del cambio de día se pueda fijar
     * desde un test.
     *
     * <p>
     * Una factura saldada <b>sale sola</b> del rango en cuanto sube
     * {@code settled_amount}, porque el marcador se recalcula con la fila.
     */
    @Query(value = """
            SELECT *
            FROM subscription_billing_documents
            WHERE overdue_marker IS NOT NULL
              AND due_date < :today
            """, countQuery = """
            SELECT COUNT(*)
            FROM subscription_billing_documents
            WHERE overdue_marker IS NOT NULL
              AND due_date < :today
            """, nativeQuery = true)
    Page<SubscriptionBillingDocumentJpaEntity> findAllOverdue(@Param("today") LocalDate today,
            Pageable pageable);

    @Query(value = """
            SELECT *
            FROM subscription_billing_documents
            WHERE subscription_id = :subscriptionId
              AND company_id = :companyId
              AND document_kind = 'INVOICE'
              AND issue_status = 'EXTERNAL_REGISTERED'
              AND balance_amount > 0
              AND due_date < :today
            ORDER BY due_date, id
            LIMIT 1
            """, nativeQuery = true)
    Optional<SubscriptionBillingDocumentJpaEntity> findOldestOverdue(
            @Param("subscriptionId") Long subscriptionId, @Param("companyId") Long companyId,
            @Param("today") LocalDate today);

    /**
     * Reclama trabajo de cobranza sin tabla de lease. El cursor por id y
     * {@code SKIP LOCKED} permiten varios workers sin procesar la misma factura.
     */
    @Query(value = """
            SELECT *
            FROM subscription_billing_documents
            WHERE overdue_marker IS NOT NULL
              AND due_date < :today
              AND id > :afterId
            ORDER BY id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<SubscriptionBillingDocumentJpaEntity> lockOverdueBatchAfter(
            @Param("today") LocalDate today, @Param("afterId") long afterId,
            @Param("batchSize") int batchSize);

    /**
     * <b>La barandilla contra la doble facturación, por periodo exacto.</b>
     *
     * <p>
     * Réplica literal del {@code CASE} de {@code recurring_cycle_marker} más las
     * dos columnas de periodo de {@code uq_sbd_recurring_cycle}. Las tres
     * condiciones del marcador están y cada una hace falta:
     * <ul>
     * <li>{@code document_kind = 'INVOICE'} — una nota crédito del mismo periodo es
     * legítima.</li>
     * <li>{@code billing_reason = 'RECURRING_CYCLE'} — sin esto, una factura de
     * prorrateo con el mismo periodo exacto se rechazaría, bloqueando un cobro
     * legítimo.</li>
     * <li>{@code issue_status <> 'VOIDED'} — sin esto, un error en la factura de
     * septiembre haría ese periodo irrecuperable para siempre.</li>
     * </ul>
     *
     * <p>
     * Y el periodo se compara por <b>igualdad de los dos extremos</b>, nunca por
     * mes: agrupando por mes, la factura anual emitida a mitad de agosto chocaba
     * con la mensual del día 1 y el cambio a plan anual era irregistrable.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM subscription_billing_documents
            WHERE company_id = :companyId
              AND subscription_id = :subscriptionId
              AND document_kind = 'INVOICE'
              AND billing_reason = 'RECURRING_CYCLE'
              AND issue_status <> 'VOIDED'
              AND period_start = :periodStart
              AND period_end = :periodEnd
            """, nativeQuery = true)
    long countRecurringCycle(@Param("companyId") Long companyId,
            @Param("subscriptionId") Long subscriptionId,
            @Param("periodStart") LocalDate periodStart, @Param("periodEnd") LocalDate periodEnd);
}
