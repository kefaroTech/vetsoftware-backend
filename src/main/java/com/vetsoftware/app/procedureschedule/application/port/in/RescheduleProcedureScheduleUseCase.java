package com.vetsoftware.app.procedureschedule.application.port.in;

import com.vetsoftware.app.procedureschedule.application.command.RescheduleProcedureScheduleCommand;
import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RescheduleProcedureScheduleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalization.update') or hasRole('SYSTEM')")
    List<ProcedureScheduleDto> execute(RescheduleProcedureScheduleCommand command);
}
