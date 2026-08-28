package com.vetsoftware.app.paymentrefund.application.port.in;

import com.vetsoftware.app.paymentrefund.application.dto.PaymentRefundDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListPaymentRefundsUseCase {

    /**
     * Las devoluciones de una empresa. No hay hermano sin acotar en este puerto: un
     * listado que no filtra por empresa devuelve filas de todos los tenants
     * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}). El barrido cross-tenant vive
     * aparte, en {@link ListAllPaymentRefundsUseCase}, cerrado a plataforma.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('paymentRefund.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<PaymentRefundDto> listByCompany(Long companyId, int page, int pageSize);
}
