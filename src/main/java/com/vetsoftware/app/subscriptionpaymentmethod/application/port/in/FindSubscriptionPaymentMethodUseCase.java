package com.vetsoftware.app.subscriptionpaymentmethod.application.port.in;

import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSubscriptionPaymentMethodUseCase {

    /**
     * Un {@code id} lo escribe el cliente en la URL, asi que el {@code companyId}
     * viaja siempre y la carga va acotada por el en el puerto de salida. La
     * variante ancha no existe a proposito (BE-COV,
     * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}).
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscriptionPaymentMethod.read')"
            + " and @authz.isMyCompany(#companyId))")
    SubscriptionPaymentMethodDto findById(Long id, Long companyId);
}
