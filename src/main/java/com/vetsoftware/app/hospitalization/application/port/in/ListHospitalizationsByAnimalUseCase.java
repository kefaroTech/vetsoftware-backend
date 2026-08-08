package com.vetsoftware.app.hospitalization.application.port.in;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.hospitalization.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationsByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.read')")
    PageResult<HospitalizationDto> listByAnimal(Long animalId, String query, int page,
            int pageSize);
}
