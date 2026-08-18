package com.vetsoftware.app.hospitalizationmedication.application.usecase;

import com.vetsoftware.app.hospitalizationmedication.application.command.SuspendHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.SuspendHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationMedicationRepository;
import com.vetsoftware.app.hospitalizationmedication.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedication;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedicationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.medication.suspend")
@Service
public class SuspendHospitalizationMedicationService
        implements
            SuspendHospitalizationMedicationUseCase {
    private final HospitalizationMedicationRepository repository;
    private final EmployeeQueryPort employeeQueryPort;

    public SuspendHospitalizationMedicationService(HospitalizationMedicationRepository repository,
            EmployeeQueryPort employeeQueryPort) {
        this.repository = repository;
        this.employeeQueryPort = employeeQueryPort;
    }

    @Override
    @Transactional
    public HospitalizationMedicationDto execute(SuspendHospitalizationMedicationCommand command) {
        HospitalizationMedication medication = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new HospitalizationMedicationNotFoundException(command.id()));
        EmployeeRef by = employeeQueryPort.findById(command.suspendedById())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employee not found: " + command.suspendedById()));
        medication.suspend(by, LocalDateTime.now());
        return HospitalizationMedicationDto.from(repository.save(medication));
    }
}
