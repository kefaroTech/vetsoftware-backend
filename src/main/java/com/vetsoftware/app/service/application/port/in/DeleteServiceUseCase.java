package com.vetsoftware.app.service.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteServiceUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('service.delete')")
    void execute(Long id);
}
