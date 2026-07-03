package com.vetsoftware.app.laboratorytesttype.application.port.in;

import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateLaboratoryTestTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('laboratoryTest.update') or hasRole('SYSTEM')")
    LaboratoryTestTypeDto execute(Long id);
}
