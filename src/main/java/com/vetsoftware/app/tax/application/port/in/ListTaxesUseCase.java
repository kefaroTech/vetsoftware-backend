package com.vetsoftware.app.tax.application.port.in;

import com.vetsoftware.app.tax.application.dto.TaxDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListTaxesUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('tax.read') and @authz.isMyCompany(#companyId))")
    List<TaxDto> listByCompany(Long companyId);
}
