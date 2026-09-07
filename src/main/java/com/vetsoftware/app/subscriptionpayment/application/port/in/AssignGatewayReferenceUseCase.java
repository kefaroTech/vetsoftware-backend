package com.vetsoftware.app.subscriptionpayment.application.port.in;

import com.vetsoftware.app.subscriptionpayment.application.command.AssignGatewayReferenceCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AssignGatewayReferenceUseCase {

    /**
     * Asigna la referencia real de la pasarela a una reserva que nacio sin ella.
     *
     * <p>
     * <strong>SYSTEM a secas, sin camino de tenant.</strong> Mismo razonamiento que
     * {@link RegisterSubscriptionPaymentUseCase}: es tesoreria de la plataforma, no
     * una operacion del empleado de una clinica.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    SubscriptionPaymentDto execute(AssignGatewayReferenceCommand command);
}
