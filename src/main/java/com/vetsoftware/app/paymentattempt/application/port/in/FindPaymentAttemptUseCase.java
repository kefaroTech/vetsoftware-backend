package com.vetsoftware.app.paymentattempt.application.port.in;

import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindPaymentAttemptUseCase {

    /**
     * Un {@code id} lo escribe el cliente en la URL, asi que el {@code companyId}
     * viaja siempre y la carga va acotada por el en el puerto de salida. No existe
     * la variante ancha a proposito (BE-COV,
     * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}).
     *
     * <p>
     * El cliente lee lo suyo, pero <strong>nunca el codigo de rechazo crudo de la
     * pasarela</strong>: eso lo recorta {@code PaymentAttemptResponse}, no este
     * puerto.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('paymentAttempt.read')"
            + " and @authz.isMyCompany(#companyId))")
    PaymentAttemptDto findById(Long id, Long companyId);
}
