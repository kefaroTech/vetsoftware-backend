package com.vetsoftware.app.hospitalization.application.port.in;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationsByAnimalUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.read')")
  List<HospitalizationDto> listByAnimal(Long animalId);
}
