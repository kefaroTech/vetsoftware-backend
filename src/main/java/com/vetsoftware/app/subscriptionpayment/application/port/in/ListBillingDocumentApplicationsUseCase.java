package com.vetsoftware.app.subscriptionpayment.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpayment.application.dto.BillingDocumentApplicationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListBillingDocumentApplicationsUseCase {

    /** Que salda esta factura, en orden cronologico de aplicacion. */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('billingDocumentApplication.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<BillingDocumentApplicationDto> listByTargetDocument(Long targetDocumentId,
            Long companyId, int page, int pageSize);
}
