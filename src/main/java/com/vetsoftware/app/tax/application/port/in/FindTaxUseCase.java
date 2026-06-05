package com.vetsoftware.app.tax.application.port.in;

import com.vetsoftware.app.tax.application.dto.TaxDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindTaxUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('tax.read')")
    TaxDto findById(Long id);
}
