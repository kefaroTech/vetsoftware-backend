package com.vetsoftware.app.vatfilingperiod.application.port.in;

import com.vetsoftware.app.vatfilingperiod.application.command.CreateVatFilingPeriodCommand;
import com.vetsoftware.app.vatfilingperiod.application.dto.VatFilingPeriodDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateVatFilingPeriodUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    VatFilingPeriodDto execute(CreateVatFilingPeriodCommand command);
}
