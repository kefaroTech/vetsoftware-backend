package com.vetsoftware.app.subscriptionpaymentmethod.application.port.in;

import com.vetsoftware.app.subscriptionpaymentmethod.application.command.SetDefaultPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SetDefaultPaymentMethodUseCase {

    /**
     * Elige con que se cobra por defecto. <strong>Uno solo por empresa</strong>, y
     * la exclusividad la garantiza la base con la columna generada
     * {@code default_marker}: el caso de uso libera el hueco del anterior en la
     * misma transaccion para que esa garantia no se note como un 500.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscriptionPaymentMethod.update')"
            + " and @authz.isMyCompany(#command.companyId))")
    SubscriptionPaymentMethodDto execute(SetDefaultPaymentMethodCommand command);
}
