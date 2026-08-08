package com.vetsoftware.app.deworming.application.port.in;

import com.vetsoftware.app.deworming.application.command.UpdateDewormingCommand;
import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateDewormingUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('deworming.update') and @authz.isMyCompany(#command.companyId))")
    DewormingDto execute(UpdateDewormingCommand command);
}
