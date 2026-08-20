package com.vetsoftware.app.medicationschedule.application.port.in;

import com.vetsoftware.app.medicationschedule.application.command.RescheduleMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.RescheduleResultDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RescheduleMedicationScheduleUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.update')"
            + " and @authz.isMyCompany(#command.companyId))")
    RescheduleResultDto execute(RescheduleMedicationScheduleCommand command);
}
