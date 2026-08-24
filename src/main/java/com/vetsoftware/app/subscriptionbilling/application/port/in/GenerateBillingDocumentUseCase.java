package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.subscriptionbilling.application.command.GenerateBillingDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Calcular y numerar la cuenta de cobro de un contrato para un periodo exacto.
 *
 * <p>
 * Consume el consecutivo, agrupa los cargos pendientes, calcula el desglose
 * fiscal una sola vez y deja el documento en {@code DRAFT}. Todo dentro de una
 * transacción: si algo falla, el número vuelve atrás y no queda hueco.
 */
public interface GenerateBillingDocumentUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    BillingDocumentDto execute(GenerateBillingDocumentCommand command);
}
