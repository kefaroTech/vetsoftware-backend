package com.vetsoftware.app.hospitalization.application.port.in;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationsByAnimalUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalization.read') or hasRole('SYSTEM')")
    List<HospitalizationDto> listByAnimal(Long animalId);
}
