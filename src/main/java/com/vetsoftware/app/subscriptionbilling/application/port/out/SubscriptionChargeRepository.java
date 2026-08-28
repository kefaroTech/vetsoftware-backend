package com.vetsoftware.app.subscriptionbilling.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeStatus;
import com.vetsoftware.app.subscriptionbilling.domain.RecurringChargeKey;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Lo devengado.
 *
 * <p>
 * <b>No hay {@code delete} y no hay {@code findById} ancho.</b> Lo primero
 * porque un cargo no se borra ni se desactiva —{@code subscription_charges} no
 * lleva {@code enabled}—: se compensa con otro cargo negativo y los dos quedan.
 * Lo segundo porque toda carga por id de este slice va acotada por empresa: la
 * variante ancha ni siquiera se declara, así que no hay forma de llamarla por
 * descuido.
 */
public interface SubscriptionChargeRepository {

    SubscriptionCharge save(SubscriptionCharge charge);

    Optional<SubscriptionCharge> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Los cargos que un documento va a agrupar, resueltos por id y acotados por
     * empresa en la misma consulta.
     */
    List<SubscriptionCharge> findAllByIdsAndCompanyId(List<Long> ids, Long companyId);

    /**
     * <b>La consulta del proceso de facturación</b>: los cargos todavía sin
     * facturar de un contrato cuyo periodo de servicio cae dentro del periodo que
     * se va a cobrar.
     *
     * <p>
     * Sirve el índice {@code ix_subscription_charges_pending}
     * {@code (company_id, subscription_id, status, service_period_start)}.
     */
    List<SubscriptionCharge> findPendingByCompanyIdAndSubscription(Long companyId,
            Long subscriptionId, LocalDate periodStart, LocalDate periodEnd);

    /**
     * <b>La barandilla antiduplicados del barrido recurrente.</b> {@code true} si
     * la línea ya tiene su cargo {@code RECURRING} de ese periodo exacto.
     *
     * <p>
     * <b>No filtra por estado a propósito.</b> Un cargo ya {@code INVOICED} tiene
     * que seguir bloqueando el duplicado: el caso que esta consulta existe para
     * cubrir es precisamente el barrido que murió <b>después</b> de emitir la
     * factura y vuelve a arrancar.
     *
     * <p>
     * <b>Y la llave lleva la línea, no el artículo</b> —ver
     * {@link com.vetsoftware.app.subscriptionbilling.domain.RecurringChargeKey}—:
     * con tramos acumulativos un mismo artículo tiene dos líneas vivas en el mismo
     * periodo, y agruparlas dejaría de cobrar el segundo tramo.
     *
     * <p>
     * <b>No hay índice único que la respalde</b>: {@code subscription_charges} no
     * lleva columna de idempotencia. Esta comprobación es toda la barandilla, y por
     * eso su prueba es obligatoria.
     */
    boolean existsRecurringCharge(RecurringChargeKey key);

    PageResult<SubscriptionCharge> findAllByCompanyId(Long companyId, Long subscriptionId,
            ChargeStatus status, int page, int pageSize);

    /**
     * Sella los cargos dentro de su documento: {@code PENDING → INVOICED} y
     * {@code billing_document_id}.
     *
     * <p>
     * Corre dentro de la misma transacción que crea el documento. Es uno de los
     * <b>dos</b> {@code UPDATE} de la tabla —el otro es
     * {@link #releaseFromVoidedDocument}, su inverso—, y los dos son
     * <i>compare-and-set</i>: nombran el estado de partida en el {@code WHERE}, así
     * que el segundo en llegar no pisa nada y se entera contando filas. Por eso
     * {@code SubscriptionChargeJpaEntity} sigue exenta de {@code @Version} con el
     * código {@code E6_YA_PROTEGIDO}.
     *
     * @return cuántos cargos quedaron sellados; menos de los pedidos significa que
     *         alguno dejó de estar {@code PENDING} entre la lectura y el sellado
     */
    int sealAsInvoiced(List<Long> ids, Long companyId, Long billingDocumentId);

    /**
     * <b>Devuelve al circuito los cargos que un documento anulado tenía
     * sellados</b>: {@code INVOICED → PENDING} y {@code billing_document_id} a
     * nulo. Es el inverso exacto de {@link #sealAsInvoiced}.
     *
     * <p>
     * <b>El defecto que existe para cerrar.</b> Anular la cuenta de cobro dejaba
     * sus cargos en {@code INVOICED} apuntando a un documento {@code VOIDED}: el
     * ciclo siguiente no los recoge —{@code findPendingByCompanyIdAndSubscription}
     * filtra {@code status = 'PENDING'}— y no hay ninguna vigilancia que los
     * detecte, porque la tabla no tiene ni {@code enabled} ni un estado «huérfano»
     * que consultar. Es dinero devengado que no se factura jamás, y en silencio.
     *
     * <p>
     * <b>{@code PENDING} y no otro estado</b>, leyendo el modelo:
     * {@code ChargeStatus.PENDING} es «devengado y todavía sin factura, lo que
     * recoge el proceso mensual» y {@code VOIDED} es «compensado por un cargo
     * negativo». Al cargo de un documento anulado no lo compensó nadie —el
     * documento no llegó a existir fuera, que es la condición para poder anularlo—,
     * así que simplemente vuelve a estar sin facturar. Coincide con lo que ya
     * prometía {@code VoidBillingDocumentUseCase}: anular deja el periodo libre
     * para volver a emitirlo.
     *
     * <p>
     * <b>Solo toca los {@code INVOICED}.</b> Un cargo que entre medias pasó a
     * {@code VOIDED} —compensado con su fila negativa— no se resucita; y esa misma
     * condición en el {@code WHERE} hace la operación idempotente, así que anular
     * dos veces no rompe nada.
     *
     * @return cuántos cargos se liberaron
     */
    int releaseFromVoidedDocument(Long billingDocumentId, Long companyId);
}
