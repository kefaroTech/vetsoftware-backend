package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio propio de este slice sobre la entidad de otra rodaja
 * ({@code subscriptionbilling}): no hay ninguna variante existente de
 * {@code SubscriptionBillingDocumentJpaRepository} que cruce con
 * {@code payment_attempts} y {@code billing_document_applications}, así que la
 * consulta vive aquí en vez de tocar un fichero ajeno.
 *
 * <p>
 * Barrido de plataforma sin empresa delante: ver
 * {@code NewRecurringChargeQueryPort}.
 */
public interface NewRecurringChargeJpaRepository
        extends
            JpaRepository<SubscriptionBillingDocumentJpaEntity, Long> {

    /**
     * Cursor por id: documentos {@code RECURRING_CYCLE} con saldo, sin pago
     * {@code PENDING} ni {@code REFUNDED} aplicado, sin ningún intento todavía y de
     * una suscripción vigente.
     *
     * <p>
     * <strong>Un pago {@code REFUNDED} no reabre la cola</strong>: una devolución
     * de cortesía revierte la aplicación en su propia transacción, pero el
     * documento no debe volver a cobrarse solo porque quedó sin aplicación
     * {@code PENDING}.
     */
    @Query(value = """
            SELECT sbd.*
            FROM subscription_billing_documents sbd
            JOIN subscriptions s ON s.id = sbd.subscription_id
            WHERE sbd.billing_reason = 'RECURRING_CYCLE'
              AND sbd.issue_status <> 'VOIDED'
              AND sbd.balance_amount > 0
              AND sbd.id > :afterId
              AND s.status IN ('TRIALING','ACTIVE','PAST_DUE','READ_ONLY')
              AND NOT EXISTS (
                  SELECT 1 FROM payment_attempts pa
                  WHERE pa.billing_document_id = sbd.id
              )
              AND NOT EXISTS (
                  SELECT 1 FROM billing_document_applications bda
                  JOIN subscription_payments sp ON sp.id = bda.payment_id
                  WHERE bda.target_document_id = sbd.id
                    AND bda.source_kind = 'PAYMENT'
                    AND sp.status IN ('PENDING', 'REFUNDED')
              )
            ORDER BY sbd.id
            LIMIT :batchSize
            """, nativeQuery = true)
    List<SubscriptionBillingDocumentJpaEntity> findNewRecurringChargesAfter(
            @Param("afterId") long afterId, @Param("batchSize") int batchSize);
}
