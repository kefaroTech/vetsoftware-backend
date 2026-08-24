package com.vetsoftware.app.subscriptionpayment.application.port.in;

import com.vetsoftware.app.subscriptionpayment.application.command.ChangeSubscriptionPaymentStatusCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ChangeSubscriptionPaymentStatusUseCase {

    /**
     * Confirma, marca como fallido o devuelve un pago. Es la unica puerta por la
     * que un pago pasa a {@code CONFIRMED}, y por tanto la unica por la que empieza
     * a contar como cobro.
     *
     * <p>
     * <strong>SYSTEM a secas, sin camino de tenant.</strong> Decidir que un cobro
     * cuenta —o que se devuelve— es tesoreria de la plataforma; el razonamiento
     * completo y el porque no se siembra {@code subscriptionPayment.changeStatus}
     * estan en {@link RegisterSubscriptionPaymentUseCase}.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    SubscriptionPaymentDto execute(ChangeSubscriptionPaymentStatusCommand command);
}
