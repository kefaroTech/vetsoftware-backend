package com.vetsoftware.app.procedureschedule.application.usecase;

import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import com.vetsoftware.app.procedureschedule.application.port.in.ListProcedureSchedulesByHospitalizationUseCase;
import com.vetsoftware.app.procedureschedule.application.port.out.ProcedureScheduleRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "procedure.schedule.list.by.hospitalization")
@Service
public class ListProcedureSchedulesByHospitalizationService
        implements
            ListProcedureSchedulesByHospitalizationUseCase {
    private final ProcedureScheduleRepository repository;

    public ListProcedureSchedulesByHospitalizationService(ProcedureScheduleRepository repository) {
        this.repository = repository;
    }

    /**
     * {@code companyId == null} es el camino SYSTEM, cross-tenant por diseño. Un
     * empleado solo ve el plan de procedimientos de las hospitalizaciones de su
     * empresa: la hospitalización ajena devuelve la lista vacía, no un 403 que
     * confirme que existe.
     */
    @Override
    public List<ProcedureScheduleDto> listByHospitalization(Long hospitalizationId,
            Long companyId) {
        return (companyId == null
                ? repository.findByHospitalizationId(hospitalizationId)
                : repository.findByHospitalizationIdAndCompanyId(hospitalizationId, companyId))
                .stream().map(ProcedureScheduleDto::from).toList();
    }
}
