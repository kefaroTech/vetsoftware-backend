package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionChargeDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Consultar un cargo propio.
 *
 * <p>
 * Lo alcanza el empleado del tenant, y por eso recibe {@code companyId} y lo
 * valida: el {@code id} lo escribe el cliente en la URL y el permiso dice qué
 * puede hacer, nunca sobre qué filas.
 */
public interface FindSubscriptionChargeUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscriptionBilling.read') and"
            + " @authz.isMyCompany(#companyId))")
    SubscriptionChargeDto findById(Long id, Long companyId);
}
