package com.vetsoftware.app.laboratorytest.application.port.in;

import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListLaboratoryTestsUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    List<LaboratoryTestDto> listAll();
}
