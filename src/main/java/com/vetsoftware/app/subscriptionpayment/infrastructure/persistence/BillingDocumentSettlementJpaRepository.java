package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaEntity;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * La unica escritura que este slice hace sobre
 * {@code subscription_billing_documents}.
 *
 * <p>
 * Vive aqui y no en el repositorio de {@code subscriptionbilling} porque esta
 * consulta solo existe por lo que pasa en este slice: es el reverso exacto de
 * insertar o revertir una aplicacion. Extiende {@link Repository} y no
 * {@code JpaRepository} a proposito -hereda cero metodos-, asi que desde aqui
 * no hay forma de tropezarse con un {@code findAll()} sobre las facturas de
 * todos los tenants.
 */
public interface BillingDocumentSettlementJpaRepository
        extends
            Repository<SubscriptionBillingDocumentJpaEntity, Long> {

    /**
     * R4: {@code settled_amount} es <strong>siempre</strong> la suma de las
     * aplicaciones cuyo origen cuenta como cobro, recalculada de cero dentro de la
     * transaccion que la provoco.
     *
     * <p>
     * <strong>Por que un recalculo y no {@code settled_amount + x}:</strong> un
     * acumulador pierde la reconciliacion en cuanto un paso falla a medias, y no
     * hay forma de saber despues cuanto se perdio. El recalculo no puede derivar:
     * si las filas estan bien, la columna esta bien.
     *
     * <p>
     * <strong>El {@code LEFT JOIN} contra {@code subscription_payments} es la parte
     * delicada.</strong> Con un {@code JOIN} normal, las aplicaciones que no llevan
     * pago -nota credito, retencion, saldo a favor, redondeo, castigo-
     * desaparecerian de la suma, y el saldo de la factura no bajaria nunca aunque
     * se le hubiera saldado por esa via.
     *
     * <p>
     * <strong>Solo los pagos {@code CONFIRMED} cuentan, y los otros cinco origenes
     * cuentan siempre.</strong> {@code CREDIT_NOTE}, {@code WITHHOLDING},
     * {@code CUSTOMER_CREDIT}, {@code ROUNDING} y {@code WRITE_OFF} no llevan
     * {@code payment_id} ({@code chk_bda_source_exclusive}): saldan en el momento
     * en que se registran, sin pasarela de por medio que confirmar. Solo
     * {@code PAYMENT} necesita esperar la confirmacion, porque solo ahi la pasarela
     * pudo avisar sin llegar a cobrar.
     *
     * <p>
     * {@code version = version + 1} en el {@code SET} no es decorativo (#53):
     * {@code @Version} solo protege el ciclo leer-modificar-guardar de una entidad
     * gestionada, y una {@code @Query} de {@code UPDATE} va directa a la base. Sin
     * mover la version, un {@code save} concurrente que venga de una lectura
     * anterior casa igual y pisa este recalculo sin excepcion, sin log y sin 409.
     *
     * <p>
     * <strong>El {@code LEAST} contra {@code total_amount} es la mitad que evita
     * {@code chk_sbd_settled_cap}.</strong> Dos origenes {@code PAYMENT} PENDING se
     * pueden aplicar cada uno por el total del documento sin que nada lo impida
     * -ninguno cuenta todavia-; si los dos llegan a confirmarse, la suma sin tope
     * violaria la restriccion de la base. El exceso no se pierde: queda vivo en las
     * aplicaciones (R1, nunca se editan) y
     * {@code ChangeSubscriptionPaymentStatusService} lo detecta con
     * {@link #computeUncappedSettledAmount} y lo convierte en saldo a favor por
     * pago en exceso.
     *
     * <p>
     * {@code balance_amount} <strong>no aparece</strong>: es una columna calculada
     * que mantiene la base, y escribirla desde aqui es imposible por definicion.
     *
     * @return filas actualizadas: 0 significa que el documento no existe o no es de
     *         esa empresa
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            UPDATE subscription_billing_documents d
               SET d.settled_amount = LEAST(COALESCE((
                       SELECT SUM(a.applied_amount)
                         FROM billing_document_applications a
                         LEFT JOIN subscription_payments p
                                ON p.id = a.payment_id AND p.company_id = a.company_id
                        WHERE a.target_document_id = d.id
                          AND a.company_id = d.company_id
                          AND (a.source_kind IN ('CREDIT_NOTE', 'WITHHOLDING', 'CUSTOMER_CREDIT',
                                                  'ROUNDING', 'WRITE_OFF')
                               OR p.status = 'CONFIRMED')
                   ), 0), d.total_amount),
                   d.version = d.version + 1
             WHERE d.id = :documentId AND d.company_id = :companyId
            """)
    int recalculateSettledAmount(@Param("documentId") Long documentId,
            @Param("companyId") Long companyId);

    /**
     * La misma suma que {@link #recalculateSettledAmount}, <strong>sin el
     * tope</strong> y sin escribir nada: lo que de verdad se aplico, aunque exceda
     * el documento.
     */
    @Query(nativeQuery = true, value = """
            SELECT COALESCE(SUM(a.applied_amount), 0)
              FROM billing_document_applications a
              LEFT JOIN subscription_payments p
                     ON p.id = a.payment_id AND p.company_id = a.company_id
             WHERE a.target_document_id = :documentId
               AND a.company_id = :companyId
               AND (a.source_kind IN ('CREDIT_NOTE', 'WITHHOLDING', 'CUSTOMER_CREDIT',
                                       'ROUNDING', 'WRITE_OFF')
                    OR p.status = 'CONFIRMED')
            """)
    BigDecimal computeUncappedSettledAmount(@Param("documentId") Long documentId,
            @Param("companyId") Long companyId);
}
