package com.vetsoftware.app.breed.application.port.in;

import com.vetsoftware.app.breed.application.dto.BreedDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindBreedUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    BreedDto findById(Long id);
}
