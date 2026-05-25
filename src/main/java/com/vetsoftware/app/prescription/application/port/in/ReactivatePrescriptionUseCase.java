package com.vetsoftware.app.prescription.application.port.in;

import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivatePrescriptionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('prescription.update') or hasRole('SYSTEM')")
    PrescriptionDto execute(Long id);
}
