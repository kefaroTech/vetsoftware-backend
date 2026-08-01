package com.vetsoftware.app.economicactivity.application.port.in;

import com.vetsoftware.app.economicactivity.application.command.UpdateEconomicActivityCommand;
import com.vetsoftware.app.economicactivity.application.dto.EconomicActivityDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateEconomicActivityUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    EconomicActivityDto execute(UpdateEconomicActivityCommand command);
}
