package com.vetsoftware.app.laboratorytest.application.port.in;

import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListLaboratoryTestsByAnimalUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('laboratoryTest.read') or hasRole('SYSTEM')")
    List<LaboratoryTestDto> listByAnimal(Long animalId);
}
