package com.vetsoftware.app.paymentattempt.application.port.in;

import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/** Consulta cross-tenant de cobranza para la consola de plataforma. */
public interface ListAllPaymentAttemptsUseCase {

    /**
     * @param companyId
     *            filtro opcional. Vacio recorre todas las clinicas, y por eso el
     *            puerto solo lo alcanza {@code hasRole('SYSTEM')} a secas
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<PaymentAttemptDto> listAll(Long companyId, int page, int pageSize);
}
