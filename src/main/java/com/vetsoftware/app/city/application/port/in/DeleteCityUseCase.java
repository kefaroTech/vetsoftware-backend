package com.vetsoftware.app.city.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteCityUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
