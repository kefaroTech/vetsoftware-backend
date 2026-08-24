package com.vetsoftware.app.subscriptionpayment.application.port.in;

import com.vetsoftware.app.subscriptionpayment.application.command.ReverseBillingDocumentApplicationCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.BillingDocumentApplicationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReverseBillingDocumentApplicationUseCase {

    /**
     * Deshace una aplicacion equivocada <strong>sin borrar nada</strong>: crea otra
     * que la contra-aplica y las dos quedan.
     *
     * <p>
     * <strong>SYSTEM a secas, sin camino de tenant.</strong> Deshacer un cobro
     * vuelve a subir el saldo de una factura y puede devolver el contrato a mora;
     * el razonamiento completo y el porque no se siembra
     * {@code billingDocumentApplication.reverse} estan en
     * {@link RegisterSubscriptionPaymentUseCase}.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    BillingDocumentApplicationDto execute(ReverseBillingDocumentApplicationCommand command);
}
