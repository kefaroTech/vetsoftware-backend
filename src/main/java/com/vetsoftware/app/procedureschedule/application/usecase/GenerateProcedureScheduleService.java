package com.vetsoftware.app.procedureschedule.application.usecase;

import com.vetsoftware.app.procedureschedule.application.command.GenerateProcedureScheduleCommand;
import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import com.vetsoftware.app.procedureschedule.application.port.in.GenerateProcedureScheduleUseCase;
import com.vetsoftware.app.procedureschedule.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.procedureschedule.application.port.out.HospitalizationProcedureQueryPort;
import com.vetsoftware.app.procedureschedule.application.port.out.ProcedureScheduleRepository;
import com.vetsoftware.app.procedureschedule.domain.EmployeeRef;
import com.vetsoftware.app.procedureschedule.domain.ProcedureOrderParams;
import com.vetsoftware.app.procedureschedule.domain.ProcedureSchedule;
import com.vetsoftware.app.procedureschedule.domain.ProcedureScheduleGenerator;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "procedure_schedule.generate")
@Service
public class GenerateProcedureScheduleService implements GenerateProcedureScheduleUseCase {
    private final ProcedureScheduleRepository repository;
    private final HospitalizationProcedureQueryPort procedureQueryPort;
    private final EmployeeQueryPort employeeQueryPort;

    public GenerateProcedureScheduleService(ProcedureScheduleRepository repository,
                                            HospitalizationProcedureQueryPort procedureQueryPort,
                                            EmployeeQueryPort employeeQueryPort) {
        this.repository = repository;
        this.procedureQueryPort = procedureQueryPort;
        this.employeeQueryPort = employeeQueryPort;
    }

    @Override
    @Transactional
    public List<ProcedureScheduleDto> execute(GenerateProcedureScheduleCommand command) {
        ProcedureOrderParams params = procedureQueryPort.findById(command.hospitalizationProcedureId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Hospitalization procedure not found: " + command.hospitalizationProcedureId()));
        EmployeeRef createdBy = employeeQueryPort.findById(command.createdById())
            .orElseThrow(() -> new IllegalArgumentException(
                "Employee not found: " + command.createdById()));

        // Idempotente: limpia el plan previo antes de regenerar (cubre creación y edición).
        repository.disableByHospitalizationProcedureId(params.id());

        List<ProcedureSchedule> generated = ProcedureScheduleGenerator.generate(params, createdBy);
        return generated.stream()
            .map(repository::save)
            .map(ProcedureScheduleDto::from)
            .toList();
    }
}
