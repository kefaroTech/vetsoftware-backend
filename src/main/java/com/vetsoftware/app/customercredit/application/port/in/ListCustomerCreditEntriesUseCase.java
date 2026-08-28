package com.vetsoftware.app.customercredit.application.port.in;

import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListCustomerCreditEntriesUseCase {

    /**
     * El libro de una empresa. No hay hermano sin acotar en este puerto: un listado
     * que no filtra por empresa devuelve filas de todos los tenants
     * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}). Los barridos de plataforma viven
     * en {@link ListAllCustomerCreditEntriesUseCase} y en
     * {@link ListExpiringCustomerCreditUseCase}, cerrados a SYSTEM.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('customerCredit.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<CustomerCreditEntryDto> listByCompany(Long companyId, int page, int pageSize);
}
