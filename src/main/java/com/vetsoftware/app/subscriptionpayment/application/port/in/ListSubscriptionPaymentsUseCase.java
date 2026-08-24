package com.vetsoftware.app.subscriptionpayment.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSubscriptionPaymentsUseCase {

    /**
     * Los pagos de una empresa. No hay hermano sin acotar: un listado que no filtra
     * por empresa devuelve filas de todos los tenants
     * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}). La consola de plataforma llega
     * aqui igual, con la cabecera {@code X-Company-Id} que resuelve
     * {@code authz.currentCompanyId()}.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscriptionPayment.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<SubscriptionPaymentDto> listByCompany(Long companyId, int page, int pageSize);
}
