package com.vetsoftware.app.procedureschedule.application.port.in;

import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SuspendPendingProcedureSchedulesUseCase {
    /**
     * Soft-delete de las ejecuciones pendientes de un procedimiento; conserva las
     * aplicadas.
     *
     * <p>
     * Aquí no hay lectura previa que valide la propiedad: el servicio escribe
     * primero y decide qué devolver mirando lo que quedó vivo. Sin
     * {@code companyId} bastaba adivinar el id del procedimiento para suspenderle
     * el plan a un paciente de otro tenant. El {@code companyId} no viaja en el
     * request: lo pone el controller desde el contexto autenticado.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.update')"
            + " and @authz.isMyCompany(#companyId))")
    List<ProcedureScheduleDto> execute(Long hospitalizationProcedureId, Long companyId);
}
