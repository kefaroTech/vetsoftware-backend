package com.vetsoftware.app.procedureschedule.application.usecase;

import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import com.vetsoftware.app.procedureschedule.application.port.in.SuspendPendingProcedureSchedulesUseCase;
import com.vetsoftware.app.procedureschedule.application.port.out.ProcedureScheduleRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "procedure.schedule.suspend.pending")
@Service
public class SuspendPendingProcedureSchedulesService
        implements
            SuspendPendingProcedureSchedulesUseCase {
    private final ProcedureScheduleRepository repository;

    public SuspendPendingProcedureSchedulesService(ProcedureScheduleRepository repository) {
        this.repository = repository;
    }

    /**
     * No hay lectura previa que valide la propiedad —se escribe primero y se
     * devuelve lo que quedó vivo—, así que el {@code AND company_id} del UPDATE es
     * la única barrera. Con un procedimiento de otro tenant el UPDATE acotado no
     * toca ninguna fila y la lectura acotada devuelve vacío: ni se suspende nada ni
     * se filtra que el procedimiento existe. {@code companyId == null} es el camino
     * SYSTEM.
     */
    @Override
    @Transactional
    public List<ProcedureScheduleDto> execute(Long hospitalizationProcedureId, Long companyId) {
        if (companyId == null) {
            repository.disablePendingByHospitalizationProcedureId(hospitalizationProcedureId);
        } else {
            repository.disablePendingByHospitalizationProcedureId(hospitalizationProcedureId,
                    companyId);
        }
        // Quedan solo las aplicadas (enabled=true).
        return (companyId == null
                ? repository.findByHospitalizationProcedureId(hospitalizationProcedureId)
                : repository.findByHospitalizationProcedureIdAndCompanyId(
                        hospitalizationProcedureId, companyId))
                .stream().map(ProcedureScheduleDto::from).toList();
    }
}
