package com.vetsoftware.app.vaccination.application.port.in;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import com.vetsoftware.app.vaccination.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListVaccinationsByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('vaccination.read')")
    PageResult<VaccinationDto> listByAnimal(Long animalId, int page, int pageSize);
}
