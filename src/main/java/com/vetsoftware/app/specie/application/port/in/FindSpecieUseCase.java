package com.vetsoftware.app.specie.application.port.in;

import com.vetsoftware.app.specie.application.dto.SpecieDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSpecieUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    SpecieDto findById(Long id);
}
