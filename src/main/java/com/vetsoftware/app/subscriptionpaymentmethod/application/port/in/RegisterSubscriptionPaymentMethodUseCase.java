package com.vetsoftware.app.subscriptionpaymentmethod.application.port.in;

import com.vetsoftware.app.subscriptionpaymentmethod.application.command.RegisterSubscriptionPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RegisterSubscriptionPaymentMethodUseCase {

    /**
     * Da de alta un medio de pago de la clinica.
     *
     * <p>
     * <strong>Lo escribe el cliente, no la plataforma</strong>, y esa es la
     * diferencia con el resto del circuito de cobro. El documento maestro lo
     * clasifica en el bloque <em>Facturacion del cliente</em> —«su perfil fiscal,
     * su tarjeta, su resolucion, sus canales: escribe el cliente, leen ambos»—
     * junto a {@code company_billing_profiles} (316) y
     * {@code company_contact_channels} (318), entre los que nace su changeset
     * (319). Las devoluciones, los intentos y el saldo a favor si son del bloque
     * <em>Cobro y saldos</em>, que escribe solo la plataforma; este no.
     *
     * <p>
     * De ahi que el request no lleve la empresa y la inyecte el servidor desde
     * quien firma, y que el {@code @PreAuthorize} revalide con {@code isMyCompany}
     * como defensa en profundidad.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscriptionPaymentMethod.create')"
            + " and @authz.isMyCompany(#command.companyId))")
    SubscriptionPaymentMethodDto execute(RegisterSubscriptionPaymentMethodCommand command);
}
