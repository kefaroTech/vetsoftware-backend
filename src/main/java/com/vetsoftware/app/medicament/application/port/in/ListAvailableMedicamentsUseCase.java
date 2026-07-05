package com.vetsoftware.app.medicament.application.port.in;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListAvailableMedicamentsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('prescription.read') or hasAuthority('prescription.create') or hasRole('SYSTEM')")
    List<MedicamentDto> listAvailable(Long companyId);
}
