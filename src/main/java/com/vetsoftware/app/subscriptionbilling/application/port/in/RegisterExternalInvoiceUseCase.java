package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.subscriptionbilling.application.command.RegisterExternalInvoiceCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Capturar aquí la referencia de la factura emitida fuera, y con ella el
 * vencimiento contado desde <b>la fecha fiscal</b>.
 *
 * <p>
 * Es el paso manual del circuito y deja su rastro:
 * {@code external_registered_by_system_user_id} y
 * {@code external_registered_at}. Solo se hace una vez — a partir de ahí el
 * importe queda sellado.
 */
public interface RegisterExternalInvoiceUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    BillingDocumentDto execute(RegisterExternalInvoiceCommand command);
}
