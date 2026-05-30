package com.vetsoftware.app.laboratorytestfile.application.port.in;

import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListLaboratoryTestFilesByLaboratoryTestUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('laboratoryTestFile.read') or hasRole('SYSTEM')")
    List<LaboratoryTestFileDto> listByLaboratoryTest(Long laboratoryTestId);
}
