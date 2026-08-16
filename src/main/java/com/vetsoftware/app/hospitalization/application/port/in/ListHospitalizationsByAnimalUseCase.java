package com.vetsoftware.app.hospitalization.application.port.in;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationsByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.read') and @authz.isMyCompany(#companyId))")
    PageResult<HospitalizationDto> listByAnimal(Long animalId, Long companyId, String query,
            int page, int pageSize);
}
