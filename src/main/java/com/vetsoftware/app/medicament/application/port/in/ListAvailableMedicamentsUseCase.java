package com.vetsoftware.app.medicament.application.port.in;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListAvailableMedicamentsUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('prescription.read') or"
            + " hasAuthority('prescription.create')")
    List<MedicamentDto> listAvailable(Long companyId);
}
