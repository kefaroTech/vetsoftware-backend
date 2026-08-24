package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.subscriptionbilling.application.command.CreateSubscriptionChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionChargeDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Devengar un cargo contra el contrato de una clínica.
 *
 * <p>
 * <b>Cerrado a {@code hasRole("SYSTEM")} a secas, y no por comodidad.</b> Quien
 * factura aquí es la plataforma, no la clínica: un empleado del tenant no puede
 * escribir en su propio devengado, igual que un cliente no se emite a sí mismo
 * la factura de su proveedor. El {@code companyId} viaja en el command porque
 * el cargo tiene que colgar de una empresa concreta —y porque la FK compuesta
 * lo exige—, no porque lo elija un empleado.
 */
public interface CreateSubscriptionChargeUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    SubscriptionChargeDto execute(CreateSubscriptionChargeCommand command);
}
