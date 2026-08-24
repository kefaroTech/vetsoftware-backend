package com.vetsoftware.app.subscriptionpayment.application.port.in;

import com.vetsoftware.app.subscriptionpayment.application.command.ApplyBillingDocumentCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.BillingDocumentApplicationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ApplyBillingDocumentUseCase {

    /**
     * Aplica un pago o un saldo a favor contra una factura y recalcula el
     * {@code settled_amount} del destino en la misma transaccion (R4).
     *
     * <p>
     * <strong>SYSTEM a secas, sin camino de tenant.</strong> Es la operacion que
     * mueve el saldo de la cartera y la que dispara la reevaluacion de mora; el
     * razonamiento completo y el porque no se siembra
     * {@code billingDocumentApplication.create} estan en
     * {@link RegisterSubscriptionPaymentUseCase}.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    BillingDocumentApplicationDto execute(ApplyBillingDocumentCommand command);
}
