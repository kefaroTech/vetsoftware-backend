package com.vetsoftware.app.subscriptionpaymentmethod.application.port.in;

import com.vetsoftware.app.subscriptionpaymentmethod.application.command.RevokeSubscriptionPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RevokeSubscriptionPaymentMethodUseCase {

    /**
     * El cliente retira la autorizacion para cobrarle.
     *
     * <p>
     * <strong>Es un derecho, no un tramite</strong>: la ley permite revocar el
     * debito automatico en cualquier momento y sin justificar, asi que el camino
     * del tenant tiene que existir y no puede quedar detras de una gestion de
     * plataforma. El motivo se guarda para poder explicarlo despues, no como
     * condicion para aceptarlo.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscriptionPaymentMethod.update')"
            + " and @authz.isMyCompany(#command.companyId))")
    SubscriptionPaymentMethodDto execute(RevokeSubscriptionPaymentMethodCommand command);
}
