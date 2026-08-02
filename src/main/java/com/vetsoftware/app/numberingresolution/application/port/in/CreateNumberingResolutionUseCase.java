package com.vetsoftware.app.numberingresolution.application.port.in;

import com.vetsoftware.app.numberingresolution.application.command.CreateNumberingResolutionCommand;
import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateNumberingResolutionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('electronicbilling.create') and @authz.isMyCompany(#command.companyId))")
    NumberingResolutionDto execute(CreateNumberingResolutionCommand command);
}
