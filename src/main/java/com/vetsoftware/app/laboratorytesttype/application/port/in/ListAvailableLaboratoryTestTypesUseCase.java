package com.vetsoftware.app.laboratorytesttype.application.port.in;

import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListAvailableLaboratoryTestTypesUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('laboratoryTest.read')")
    List<LaboratoryTestTypeDto> listAvailable(Long companyId);
}
