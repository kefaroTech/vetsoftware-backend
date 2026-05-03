package com.vetsoftware.app.breed.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteBreedUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
