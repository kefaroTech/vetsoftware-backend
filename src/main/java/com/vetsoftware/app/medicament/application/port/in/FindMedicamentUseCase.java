package com.vetsoftware.app.medicament.application.port.in;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindMedicamentUseCase {
    @PreAuthorize("hasRole('SYSTEM') or ((hasAuthority('admin.all') or hasAuthority('prescription.read')) and @authz.isMyCompany(#companyId))")
    MedicamentDto findById(Long id, Long companyId);
}
