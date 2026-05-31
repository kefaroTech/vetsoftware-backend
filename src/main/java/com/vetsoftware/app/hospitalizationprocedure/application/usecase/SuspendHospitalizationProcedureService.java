package com.vetsoftware.app.hospitalizationprocedure.application.usecase;

import com.vetsoftware.app.hospitalizationprocedure.application.command.SuspendHospitalizationProcedureCommand;
import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.SuspendHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import com.vetsoftware.app.hospitalizationprocedure.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedure;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedureNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization_procedure.suspend")
@Service
public class SuspendHospitalizationProcedureService implements SuspendHospitalizationProcedureUseCase {
    private final HospitalizationProcedureRepository repository;
    private final EmployeeQueryPort employeeQueryPort;

    public SuspendHospitalizationProcedureService(HospitalizationProcedureRepository repository,
                                                  EmployeeQueryPort employeeQueryPort) {
        this.repository = repository;
        this.employeeQueryPort = employeeQueryPort;
    }

    @Override
    @Transactional
    public HospitalizationProcedureDto execute(SuspendHospitalizationProcedureCommand command) {
        HospitalizationProcedure procedure = repository.findById(command.id())
            .orElseThrow(() -> new HospitalizationProcedureNotFoundException(command.id()));
        EmployeeRef by = employeeQueryPort.findById(command.suspendedById())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.suspendedById()));
        procedure.suspend(by, LocalDateTime.now());
        return HospitalizationProcedureDto.from(repository.save(procedure));
    }
}
