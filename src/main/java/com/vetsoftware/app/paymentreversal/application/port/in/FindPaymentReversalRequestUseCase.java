package com.vetsoftware.app.paymentreversal.application.port.in;

import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindPaymentReversalRequestUseCase {

    /**
     * Un {@code id} lo escribe el cliente en la URL, asi que el {@code companyId}
     * viaja siempre y la carga va acotada por el en el puerto de salida. No existe
     * la variante ancha a proposito (BE-COV,
     * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}).
     *
     * <p>
     * El cliente ve <strong>lo suyo</strong>: el bloque «Cobro y saldos» lo escribe
     * la plataforma y lo leen ambos.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('paymentReversalRequest.read')"
            + " and @authz.isMyCompany(#companyId))")
    PaymentReversalRequestDto findById(Long id, Long companyId);
}
