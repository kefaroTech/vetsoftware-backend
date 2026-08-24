package com.vetsoftware.app.subscriptionpayment.application.port.in;

import com.vetsoftware.app.subscriptionpayment.application.command.ReconcileSubscriptionPaymentCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReconcileSubscriptionPaymentUseCase {

    /**
     * Marca el pago como cuadrado contra el extracto bancario. Lo no conciliado es
     * lo que hay que revisar cada mes, asi que esta fecha no es un adorno: es la
     * lista de trabajo pendiente de tesoreria.
     *
     * <p>
     * <strong>SYSTEM a secas, sin camino de tenant.</strong> El extracto contra el
     * que se cuadra es el de la plataforma, no el de la clinica; el razonamiento
     * completo y el porque no se siembra {@code subscriptionPayment.reconcile}
     * estan en {@link RegisterSubscriptionPaymentUseCase}.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    SubscriptionPaymentDto execute(ReconcileSubscriptionPaymentCommand command);
}
