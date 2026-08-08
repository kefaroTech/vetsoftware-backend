package com.vetsoftware.app.laboratorytest.application.port.in;

import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListLaboratoryTestsByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('laboratoryTest.read')")
    PageResult<LaboratoryTestDto> listByAnimal(Long animalId, int page, int pageSize);
}
