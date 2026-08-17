package com.vetsoftware.app.vaccination.application.port.in;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListVaccinationsByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('vaccination.read') and @authz.isMyCompany(#companyId))")
    PageResult<VaccinationDto> listByAnimal(Long animalId, Long companyId, String query, int page,
            int pageSize);
}
