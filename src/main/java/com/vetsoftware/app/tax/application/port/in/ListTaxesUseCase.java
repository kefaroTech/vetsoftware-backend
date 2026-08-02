package com.vetsoftware.app.tax.application.port.in;

import com.vetsoftware.app.tax.application.dto.TaxDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListTaxesUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('tax.read') and @authz.isMyCompany(#companyId))")
    List<TaxDto> listByCompany(Long companyId);

    /** Lista los impuestos PAUSADOS (enabled=false) de la empresa, para el flujo de reactivación. */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('tax.read') and @authz.isMyCompany(#companyId))")
    List<TaxDto> listDisabledByCompany(Long companyId);
}
