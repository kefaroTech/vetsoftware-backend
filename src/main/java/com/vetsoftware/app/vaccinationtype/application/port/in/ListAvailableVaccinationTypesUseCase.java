package com.vetsoftware.app.vaccinationtype.application.port.in;

import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListAvailableVaccinationTypesUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('vaccination.read') or hasRole('SYSTEM')")
    List<VaccinationTypeDto> listAvailable(Long companyId);
}
