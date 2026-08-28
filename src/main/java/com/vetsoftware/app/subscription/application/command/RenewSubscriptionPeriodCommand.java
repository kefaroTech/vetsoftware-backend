package com.vetsoftware.app.subscription.application.command;

import java.time.LocalDate;

/**
 * Mover el periodo facturado del contrato al siguiente.
 *
 * <p>
 * Las tres fechas llegan <b>ya calculadas</b> por quien factura, y eso es
 * deliberado: el ancla que decide el dia del mes vive en la rodaja de
 * facturacion —{@code subscriptionbilling.domain.BillingAnchor}— y recalcularla
 * aqui crearia dos versiones de la misma regla que se desincronizarian el
 * primer febrero.
 *
 * @param nextBillingDate
 *            el dia siguiente al final del periodo. Es el ancla materializada
 *            sobre el mes que viene, no {@code periodStart} mas un mes: la
 *            diferencia es que un contrato anclado al 31 vuelve al 31 despues
 *            de haber facturado el 28 de febrero, en vez de quedarse en el 28
 *            para siempre
 */
public record RenewSubscriptionPeriodCommand(Long subscriptionId, Long companyId,
        LocalDate periodStart, LocalDate periodEnd, LocalDate nextBillingDate) {
}
