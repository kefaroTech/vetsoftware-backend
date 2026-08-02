package com.vetsoftware.app.procedureschedule.application.port.in;

import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SuspendPendingProcedureSchedulesUseCase {
    /**
     * Soft-delete de las ejecuciones pendientes de un procedimiento; conserva las
     * aplicadas.
     */
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.update')")
    List<ProcedureScheduleDto> execute(Long hospitalizationProcedureId);
}
