package com.vetsoftware.app.laboratorytesttype.application.port.in;

import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateLaboratoryTestTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('laboratoryTest.update')")
    LaboratoryTestTypeDto execute(Long id);
}
