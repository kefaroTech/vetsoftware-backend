package com.vetsoftware.app.procedureschedule.application.port.in;

import com.vetsoftware.app.procedureschedule.application.command.GenerateProcedureScheduleCommand;
import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GenerateProcedureScheduleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalization.create') or hasRole('SYSTEM')")
    List<ProcedureScheduleDto> execute(GenerateProcedureScheduleCommand command);
}
