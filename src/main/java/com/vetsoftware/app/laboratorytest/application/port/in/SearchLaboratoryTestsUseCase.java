package com.vetsoftware.app.laboratorytest.application.port.in;

import com.vetsoftware.app.laboratorytest.application.command.SearchLaboratoryTestsCommand;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SearchLaboratoryTestsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('laboratoryTest.read') and @authz.isMyCompany(#command.companyId)) or "
        + "hasRole('SYSTEM')")
    PageResult<LaboratoryTestDto> execute(SearchLaboratoryTestsCommand command);
}
