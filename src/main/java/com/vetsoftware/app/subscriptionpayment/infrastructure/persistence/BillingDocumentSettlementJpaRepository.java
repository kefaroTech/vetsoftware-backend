package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaEntity;
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
     * delicada.</strong> Con un {@code JOIN} normal, las aplicaciones de nota
     * credito -que tienen {@code payment_id} nulo- desaparecerian de la suma, y el
     * saldo de la factura no bajaria nunca aunque se le hubiera devuelto el dinero
     * al cliente. Ese es exactamente el fallo que dejaba a una clinica en solo
     * lectura por una deuda que ya no existia.
     *
     * <p>
     * <strong>Solo los pagos {@code CONFIRMED} cuentan.</strong> Un pago
     * {@code PENDING} aplicado no reduce el saldo: la pasarela aviso pero no
     * confirmo.
     *
     * <p>
     * {@code version = version + 1} en el {@code SET} no es decorativo (#53):
     * {@code @Version} solo protege el ciclo leer-modificar-guardar de una entidad
     * gestionada, y una {@code @Query} de {@code UPDATE} va directa a la base. Sin
     * mover la version, un {@code save} concurrente que venga de una lectura
     * anterior casa igual y pisa este recalculo sin excepcion, sin log y sin 409.
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
               SET d.settled_amount = COALESCE((
                       SELECT SUM(a.applied_amount)
                         FROM billing_document_applications a
                         LEFT JOIN subscription_payments p
                                ON p.id = a.payment_id AND p.company_id = a.company_id
                        WHERE a.target_document_id = d.id
                          AND a.company_id = d.company_id
                          AND (a.source_kind = 'CREDIT_NOTE' OR p.status = 'CONFIRMED')
                   ), 0),
                   d.version = d.version + 1
             WHERE d.id = :documentId AND d.company_id = :companyId
            """)
    int recalculateSettledAmount(@Param("documentId") Long documentId,
            @Param("companyId") Long companyId);
}
