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

    @Override
    public List<ProcedureScheduleDto> listByHospitalization(Long hospitalizationId) {
        return repository.findByHospitalizationId(hospitalizationId).stream()
                .map(ProcedureScheduleDto::from).toList();
    }
}
