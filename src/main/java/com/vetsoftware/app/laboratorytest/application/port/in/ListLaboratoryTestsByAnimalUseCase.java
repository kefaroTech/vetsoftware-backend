package com.vetsoftware.app.laboratorytest.application.port.in;

import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListLaboratoryTestsByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('laboratoryTest.read') and @authz.isMyCompany(#companyId))")
    PageResult<LaboratoryTestDto> listByAnimal(Long animalId, Long companyId, String query,
            int page, int pageSize);
}
