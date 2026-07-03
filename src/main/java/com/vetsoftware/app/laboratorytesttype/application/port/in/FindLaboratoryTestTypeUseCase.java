package com.vetsoftware.app.laboratorytesttype.application.port.in;

import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindLaboratoryTestTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('laboratoryTest.read') or hasRole('SYSTEM')")
    LaboratoryTestTypeDto findById(Long id);
}
