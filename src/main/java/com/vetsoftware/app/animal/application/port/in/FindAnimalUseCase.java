package com.vetsoftware.app.animal.application.port.in;

import com.vetsoftware.app.animal.application.dto.AnimalDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('animal.read') and @authz.isMyCompany(#companyId))")
    AnimalDto findById(Long id, Long companyId);
}
