package com.vetsoftware.app.openaccount.application.port.in;

import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('openAccount.read')")
    OpenAccountDto findById(Long id, Long companyId);
}
