package com.vetsoftware.app.subscriptionpaymentmethod.application.port.in;

import com.vetsoftware.app.subscriptionpaymentmethod.application.command.ExpireSubscriptionPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ExpireSubscriptionPaymentMethodUseCase {

    /**
     * Marca caducado el mandato de una tarjeta vencida.
     *
     * <p>
     * <strong>Cerrado a plataforma a secas.</strong> Lo dispara el barrido que
     * recorre {@code ix_subscription_payment_methods_expiring}, y no hay camino de
     * tenant a proposito: que una tarjeta este vencida es un hecho del calendario
     * que constata el proceso de cobro, no algo que el cliente declare sobre si
     * mismo. El cliente lo que necesita es <em>enterarse antes</em>, y eso lo
     * resuelve el aviso, no este puerto.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    SubscriptionPaymentMethodDto execute(ExpireSubscriptionPaymentMethodCommand command);
}
