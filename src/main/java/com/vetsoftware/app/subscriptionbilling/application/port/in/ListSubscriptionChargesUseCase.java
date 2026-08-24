package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionChargeDto;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeStatus;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El devengado de una clínica, paginado y <b>siempre acotado por su
 * empresa</b>.
 *
 * <p>
 * No existe la variante sin filtro para el tenant: un listado que no nombra la
 * empresa devuelve filas de todas ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}).
 */
public interface ListSubscriptionChargesUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscriptionBilling.read') and"
            + " @authz.isMyCompany(#companyId))")
    PageResult<SubscriptionChargeDto> listByCompany(Long companyId, Long subscriptionId,
            ChargeStatus status, int page, int pageSize);
}
