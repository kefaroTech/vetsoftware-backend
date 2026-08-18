package com.vetsoftware.app.medicationschedule.application.port.in;

import com.vetsoftware.app.medicationschedule.application.command.RescheduleMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RescheduleMedicationScheduleUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.update')"
            + " and @authz.isMyCompany(#command.companyId))")
    List<MedicationScheduleDto> execute(RescheduleMedicationScheduleCommand command);
}
