package com.vetsoftware.app.openaccount.application.port.in;

import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateOpenAccountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('openAccount.delete') and @authz.isMyCompany(#companyId)) or "
        + "hasRole('SYSTEM')")
    OpenAccountDto execute(Long id, Long companyId);
}
