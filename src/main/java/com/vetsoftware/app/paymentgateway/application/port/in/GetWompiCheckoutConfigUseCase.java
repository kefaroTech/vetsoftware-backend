package com.vetsoftware.app.paymentgateway.application.port.in;

import com.vetsoftware.app.paymentgateway.application.dto.WompiCheckoutConfigDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Lo que el navegador necesita para tokenizar una tarjeta directo contra Wompi.
 * Mismo permiso que dar de alta el medio de pago: quien puede llegar a pagar es
 * quien puede ver cómo se paga.
 */
public interface GetWompiCheckoutConfigUseCase {

    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('subscriptionPaymentMethod.create')")
    WompiCheckoutConfigDto execute();
}
