package com.vetsoftware.app.laboratorytest.application.port.in;

import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateLaboratoryTestUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('laboratoryTest.update') or hasRole('SYSTEM')")
    LaboratoryTestDto execute(Long id);
}
