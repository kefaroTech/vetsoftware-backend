package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** Consultar una cuenta de cobro propia, con su desglose fiscal. */
public interface FindBillingDocumentUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscriptionBilling.read') and"
            + " @authz.isMyCompany(#companyId))")
    BillingDocumentDto findById(Long id, Long companyId);
}
