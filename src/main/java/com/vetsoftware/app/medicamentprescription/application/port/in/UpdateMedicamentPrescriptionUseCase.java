package com.vetsoftware.app.medicamentprescription.application.port.in;

import com.vetsoftware.app.medicamentprescription.application.command.UpdateMedicamentPrescriptionCommand;
import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateMedicamentPrescriptionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('medicamentPrescription.update') and @authz.isMyCompany(#command.companyId))")
    MedicamentPrescriptionDto execute(UpdateMedicamentPrescriptionCommand command);
}
