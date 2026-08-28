package com.vetsoftware.app.paymentattempt.application.port.in;

import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListPaymentAttemptsByDocumentUseCase {

    /**
     * El historial de intentos de una factura: es lo que sostiene "se intento
     * cuatro veces" antes de degradar a nadie.
     *
     * <p>
     * <strong>Acotar por {@code billingDocumentId} no basta y por eso viaja tambien
     * la empresa.</strong> Una FK ajena no es un filtro de tenant -el documento es
     * de alguien-, que es el criterio de BE-29 y el mismo por el que
     * {@code findAllByAnimalId} no cuenta.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('paymentAttempt.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<PaymentAttemptDto> listByDocumentAndCompany(Long billingDocumentId, Long companyId,
            int page, int pageSize);
}
