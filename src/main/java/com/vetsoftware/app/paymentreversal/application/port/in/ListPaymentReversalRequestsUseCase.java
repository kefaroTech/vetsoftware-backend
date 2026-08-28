package com.vetsoftware.app.paymentreversal.application.port.in;

import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListPaymentReversalRequestsUseCase {

    /**
     * Los expedientes de una empresa, y el <strong>hermano acotado</strong> del
     * barrido de {@link ListExpiringReversalRequestsUseCase}.
     *
     * <p>
     * No hay variante sin filtro aqui: un listado que no acota por empresa devuelve
     * filas de todos los tenants ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}). La
     * consola de plataforma llega igual, con la cabecera {@code X-Company-Id} que
     * resuelve {@code authz.currentCompanyId()}.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('paymentReversalRequest.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<PaymentReversalRequestDto> listByCompany(Long companyId, int page, int pageSize);
}
