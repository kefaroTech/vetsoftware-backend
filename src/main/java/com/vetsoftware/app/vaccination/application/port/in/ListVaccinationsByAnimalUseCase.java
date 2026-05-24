package com.vetsoftware.app.vaccination.application.port.in;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListVaccinationsByAnimalUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('vaccination.read') or hasRole('SYSTEM')")
    List<VaccinationDto> listByAnimal(Long animalId);
}
