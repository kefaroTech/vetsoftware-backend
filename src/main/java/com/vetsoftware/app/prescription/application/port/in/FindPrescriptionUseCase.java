package com.vetsoftware.app.prescription.application.port.in;

import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindPrescriptionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('prescription.read') or hasRole('SYSTEM')")
    PrescriptionDto findById(Long id);
}
