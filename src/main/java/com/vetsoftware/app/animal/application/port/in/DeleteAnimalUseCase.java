package com.vetsoftware.app.animal.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteAnimalUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('animal.delete'))")
    void execute(Long id, Long companyId);
}
