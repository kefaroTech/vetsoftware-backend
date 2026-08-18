package com.vetsoftware.app.medicationschedule.application.port.in;

import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SuspendPendingMedicationSchedulesUseCase {
    /**
     * Soft-delete de las tomas pendientes de una medicación; conserva las
     * aplicadas.
     *
     * <p>
     * Aquí no hay lectura previa que valide la propiedad: el servicio escribe
     * primero y decide qué devolver mirando lo que quedó vivo. Sin
     * {@code companyId} bastaba adivinar el id de la orden para suspenderle la
     * medicación a un paciente de otro tenant. El {@code companyId} no viaja en el
     * request: lo pone el controller desde el contexto autenticado.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.update')"
            + " and @authz.isMyCompany(#companyId))")
    List<MedicationScheduleDto> execute(Long hospitalizationMedicationId, Long companyId);
}
