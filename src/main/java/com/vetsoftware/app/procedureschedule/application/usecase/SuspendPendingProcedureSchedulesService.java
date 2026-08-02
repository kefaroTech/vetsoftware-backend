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

    @Override
    @Transactional
    public List<ProcedureScheduleDto> execute(Long hospitalizationProcedureId) {
        repository.disablePendingByHospitalizationProcedureId(hospitalizationProcedureId);
        return repository.findByHospitalizationProcedureId(hospitalizationProcedureId).stream()
                .map(ProcedureScheduleDto::from).toList();
    }
}
