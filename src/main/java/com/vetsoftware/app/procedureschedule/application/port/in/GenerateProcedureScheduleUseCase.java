package com.vetsoftware.app.procedureschedule.application.port.in;

import com.vetsoftware.app.procedureschedule.application.command.GenerateProcedureScheduleCommand;
import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GenerateProcedureScheduleUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.create')")
    List<ProcedureScheduleDto> execute(GenerateProcedureScheduleCommand command);
}
