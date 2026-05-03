package com.vetsoftware.app.country.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteCountryUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
