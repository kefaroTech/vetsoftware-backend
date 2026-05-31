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
import com.vetsoftware.app.procedureschedule.domain.AppliedStatus;
import com.vetsoftware.app.procedureschedule.domain.ProcedureScheduleGenerator;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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

        // Regla de integridad: las ejecuciones APLICADAS son histórico inmutable; solo se
        // recalculan las pendientes.
        List<ProcedureSchedule> applied = repository.findByHospitalizationProcedureId(params.id()).stream()
            .filter(s -> s.getAppliedStatus() == AppliedStatus.APPLIED)
            .toList();

        List<ProcedureSchedule> result = new ArrayList<>();
        if (applied.isEmpty()) {
            // Alta nueva o sin aplicadas: regeneración completa (idempotente).
            repository.disableByHospitalizationProcedureId(params.id());
            for (ProcedureSchedule s : ProcedureScheduleGenerator.generate(params, createdBy)) {
                result.add(repository.save(s));
            }
        } else {
            // Conserva las aplicadas; reconstruye solo las pendientes.
            repository.disablePendingByHospitalizationProcedureId(params.id());
            LocalDateTime lastApplied = applied.stream()
                .map(ProcedureSchedule::getRealDateTime)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
            List<ProcedureSchedule> pending = ProcedureScheduleGenerator.generatePending(
                params, applied.size(), lastApplied, createdBy);
            result.addAll(applied);
            for (ProcedureSchedule s : pending) {
                result.add(repository.save(s));
            }
        }
        return result.stream().map(ProcedureScheduleDto::from).toList();
    }
}
