package com.vetsoftware.app.openaccount.application.port.in;

import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('openAccount.delete') or hasRole('SYSTEM')")
    OpenAccountDto execute(Long id);
}
