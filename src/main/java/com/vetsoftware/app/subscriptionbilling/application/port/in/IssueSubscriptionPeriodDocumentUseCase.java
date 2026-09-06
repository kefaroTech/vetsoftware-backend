package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.subscriptionbilling.application.command.IssueSubscriptionPeriodDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.IssuedPeriodDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Emite el documento de cobro de UN contrato para UN periodo exacto: el mismo
 * {@code accrue()} + {@code issue()} que hace el barrido recurrente, sin el
 * avance de periodo.
 *
 * <p>
 * Dos llamadores comparten esta operación:
 * {@code RunSubscriptionBillingCycleUseCase} (que además avanza el calendario)
 * y el cobro anticipado del primer periodo desde la pasarela de pago, que
 * necesita el documento sin tocar ningún calendario porque el contrato todavía
 * no tiene uno.
 *
 * <p>
 * Un periodo ya facturado ({@code DuplicateBillingCycleException}) o sin cargos
 * pendientes ({@code EmptyBillingDocumentException}) son desenlaces normales:
 * se devuelven como {@code issued = false}, nunca se propagan.
 */
public interface IssueSubscriptionPeriodDocumentUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    IssuedPeriodDocumentDto execute(IssueSubscriptionPeriodDocumentCommand command);
}
