package com.vetsoftware.app.paymentattempt.application.port.in;

import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListPaymentAttemptsUseCase {

    /**
     * Los intentos de una empresa, y el <strong>hermano acotado</strong> de
     * {@link ListDuePaymentAttemptsUseCase}: lo que el barrido de plataforma ve de
     * todas las clinicas, esto lo sirve de una sola
     * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}).
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('paymentAttempt.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<PaymentAttemptDto> listByCompany(Long companyId, int page, int pageSize);
}
