package com.vetsoftware.app.vaccination.application.port.in;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListVaccinationsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<VaccinationDto> listAll();
}
