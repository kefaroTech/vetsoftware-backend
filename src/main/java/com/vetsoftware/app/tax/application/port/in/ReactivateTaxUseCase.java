package com.vetsoftware.app.tax.application.port.in;

import com.vetsoftware.app.tax.application.dto.TaxDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateTaxUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('tax.delete') and @authz.isMyCompany(#companyId))")
    TaxDto execute(Long id, Long companyId);
}
