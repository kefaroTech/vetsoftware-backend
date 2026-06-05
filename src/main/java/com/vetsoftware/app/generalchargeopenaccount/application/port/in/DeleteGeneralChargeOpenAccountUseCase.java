package com.vetsoftware.app.generalchargeopenaccount.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteGeneralChargeOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('chargeOpenAccount.delete') or "
        + "hasRole('SYSTEM')")
    void execute(Long id);
}
