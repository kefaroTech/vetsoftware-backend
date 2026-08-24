package com.vetsoftware.app.subscriptionpayment.application.port.in;

import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSubscriptionPaymentUseCase {

    /**
     * Un {@code id} lo escribe el cliente en la URL, asi que el {@code companyId}
     * viaja siempre y la carga va acotada por el en el puerto de salida. No existe
     * la variante ancha a proposito (BE-COV,
     * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}).
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscriptionPayment.read')"
            + " and @authz.isMyCompany(#companyId))")
    SubscriptionPaymentDto findById(Long id, Long companyId);
}
