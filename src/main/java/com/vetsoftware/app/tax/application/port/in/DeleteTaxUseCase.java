package com.vetsoftware.app.tax.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteTaxUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('tax.delete')")
    void execute(Long id);
}
