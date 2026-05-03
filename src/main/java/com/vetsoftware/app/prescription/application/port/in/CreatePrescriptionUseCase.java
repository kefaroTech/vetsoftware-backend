package com.vetsoftware.app.prescription.application.port.in;

import com.vetsoftware.app.prescription.application.command.CreatePrescriptionCommand;
import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreatePrescriptionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    PrescriptionDto execute(CreatePrescriptionCommand command);
}
