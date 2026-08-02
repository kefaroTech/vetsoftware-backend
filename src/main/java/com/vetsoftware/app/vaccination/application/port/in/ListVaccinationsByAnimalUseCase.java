package com.vetsoftware.app.vaccination.application.port.in;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListVaccinationsByAnimalUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('vaccination.read')")
  List<VaccinationDto> listByAnimal(Long animalId);
}
