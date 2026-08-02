package com.vetsoftware.app.laboratorytest.application.usecase;

import com.vetsoftware.app.laboratorytest.application.command.ChangeLaboratoryTestStatusCommand;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.port.in.ChangeLaboratoryTestStatusUseCase;
import com.vetsoftware.app.laboratorytest.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import com.vetsoftware.app.laboratorytest.domain.EmployeeRef;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestNotFoundException;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestStatus;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "laboratory.test.change.status")
@Service
public class ChangeLaboratoryTestStatusService implements ChangeLaboratoryTestStatusUseCase {
    private final LaboratoryTestRepository repository;
    private final EmployeeQueryPort employeeQueryPort;

    public ChangeLaboratoryTestStatusService(LaboratoryTestRepository repository,
            EmployeeQueryPort employeeQueryPort) {
        this.repository = repository;
        this.employeeQueryPort = employeeQueryPort;
    }

    @Override
    @Transactional
    public LaboratoryTestDto execute(ChangeLaboratoryTestStatusCommand command) {
        LaboratoryTest laboratoryTest = repository.findById(command.id())
                .orElseThrow(() -> new LaboratoryTestNotFoundException(command.id()));
        LaboratoryTestStatus newStatus = LaboratoryTestStatus
                .valueOf(command.status().toUpperCase());

        // Al validar (COMPLETED) se firma con el empleado actual; en el resto de
        // transiciones no.
        if (newStatus == LaboratoryTestStatus.PENDING_VALIDATION
                && command.processedById() != null) {
            EmployeeRef processedBy = employeeQueryPort.findById(command.processedById())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Employee not found: " + command.processedById()));
            laboratoryTest.changeStatus(newStatus, processedBy, LocalDateTime.now());
        } else {
            laboratoryTest.changeStatus(newStatus);
        }
        return LaboratoryTestDto.from(repository.save(laboratoryTest));
    }
}
