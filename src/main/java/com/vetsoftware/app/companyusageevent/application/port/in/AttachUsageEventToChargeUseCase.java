package com.vetsoftware.app.companyusageevent.application.port.in;

import com.vetsoftware.app.companyusageevent.application.command.AttachUsageEventToChargeCommand;
import com.vetsoftware.app.companyusageevent.application.dto.CompanyUsageEventDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AttachUsageEventToChargeUseCase {

    /**
     * Cuelga el hecho del cargo que lo facturo. Lo ejecuta el cierre del periodo,
     * nunca el cliente: es literalmente el acto de cobrar.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    CompanyUsageEventDto execute(AttachUsageEventToChargeCommand command);
}
