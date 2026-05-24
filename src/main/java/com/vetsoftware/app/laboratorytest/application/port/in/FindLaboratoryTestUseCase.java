package com.vetsoftware.app.laboratorytest.application.port.in;

import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindLaboratoryTestUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('laboratoryTest.read') or hasRole('SYSTEM')")
    LaboratoryTestDto findById(Long id);
}
