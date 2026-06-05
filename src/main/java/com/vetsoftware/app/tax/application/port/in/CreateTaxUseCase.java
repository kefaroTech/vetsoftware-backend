package com.vetsoftware.app.tax.application.port.in;

import com.vetsoftware.app.tax.application.command.CreateTaxCommand;
import com.vetsoftware.app.tax.application.dto.TaxDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateTaxUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('tax.create') and @authz.isMyCompany(#command.companyId))")
    TaxDto execute(CreateTaxCommand command);
}
