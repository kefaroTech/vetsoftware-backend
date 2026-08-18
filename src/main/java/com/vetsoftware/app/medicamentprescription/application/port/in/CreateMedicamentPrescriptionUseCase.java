package com.vetsoftware.app.medicamentprescription.application.port.in;

import com.vetsoftware.app.medicamentprescription.application.command.CreateMedicamentPrescriptionCommand;
import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateMedicamentPrescriptionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('medicamentPrescription.create') and"
            + " @authz.isMyCompany(#command.companyId))")
    MedicamentPrescriptionDto execute(CreateMedicamentPrescriptionCommand command);
}
