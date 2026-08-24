package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.subscriptionbilling.application.command.VoidBillingDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Anular un documento que todavía no existe fuera.
 *
 * <p>
 * Anular deja el periodo libre para volver a emitirlo —{@code issue_status <>
 * 'VOIDED'} está dentro de {@code recurring_cycle_marker} justo para eso—, así
 * que un error en la factura de septiembre no vuelve ese periodo irrecuperable.
 */
public interface VoidBillingDocumentUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    BillingDocumentDto execute(VoidBillingDocumentCommand command);
}
