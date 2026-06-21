package com.vetsoftware.app.numberingresolution.application.port.in;

import com.vetsoftware.app.numberingresolution.application.command.UpdateNumberingResolutionCommand;
import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateNumberingResolutionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('electronicbilling.update') and @authz.isMyCompany(#command.companyId))")
    NumberingResolutionDto execute(UpdateNumberingResolutionCommand command);
}
