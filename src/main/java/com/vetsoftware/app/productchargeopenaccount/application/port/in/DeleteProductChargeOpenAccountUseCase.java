package com.vetsoftware.app.productchargeopenaccount.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteProductChargeOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('productChargeOpenAccount.delete') or "
        + "hasRole('SYSTEM')")
    void execute(Long id);
}
