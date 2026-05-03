package com.vetsoftware.app.animal.application.port.in;

import com.vetsoftware.app.animal.application.dto.AnimalDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindAnimalUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    AnimalDto findById(Long id);
}
