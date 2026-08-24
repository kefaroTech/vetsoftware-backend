package com.vetsoftware.app.subscriptionbilling.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeStatus;
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

    PageResult<SubscriptionCharge> findAllByCompanyId(Long companyId, Long subscriptionId,
            ChargeStatus status, int page, int pageSize);

    /**
     * Sella los cargos dentro de su documento: {@code PENDING → INVOICED} y
     * {@code billing_document_id}.
     *
     * <p>
     * Es el <b>único</b> {@code UPDATE} de la tabla, corre dentro de la misma
     * transacción que crea el documento y por eso
     * {@code SubscriptionChargeJpaEntity} va exenta de {@code @Version} con el
     * código {@code E6_YA_PROTEGIDO}.
     *
     * @return cuántos cargos quedaron sellados; menos de los pedidos significa que
     *         alguno dejó de estar {@code PENDING} entre la lectura y el sellado
     */
    int sealAsInvoiced(List<Long> ids, Long companyId, Long billingDocumentId);
}
