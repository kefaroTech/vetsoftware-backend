package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** Las cuentas de cobro de una clínica, paginadas y acotadas por su empresa. */
public interface ListBillingDocumentsUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscriptionBilling.read') and"
            + " @authz.isMyCompany(#companyId))")
    PageResult<BillingDocumentDto> listByCompany(Long companyId, int page, int pageSize);
}
