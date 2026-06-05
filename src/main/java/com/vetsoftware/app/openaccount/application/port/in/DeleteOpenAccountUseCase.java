package com.vetsoftware.app.openaccount.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('openAccount.delete') or hasRole('SYSTEM')")
    void execute(Long id);
}
