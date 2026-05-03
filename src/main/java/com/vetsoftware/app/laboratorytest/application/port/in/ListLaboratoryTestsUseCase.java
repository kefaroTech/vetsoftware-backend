package com.vetsoftware.app.laboratorytest.application.port.in;

import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListLaboratoryTestsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<LaboratoryTestDto> listAll();
}
