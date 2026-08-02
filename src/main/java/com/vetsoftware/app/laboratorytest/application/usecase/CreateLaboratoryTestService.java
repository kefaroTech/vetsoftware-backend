package com.vetsoftware.app.laboratorytest.application.usecase;

import com.vetsoftware.app.laboratorytest.application.command.CreateLaboratoryTestCommand;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.port.in.CreateLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.BranchQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestTypeQueryPort;
import com.vetsoftware.app.laboratorytest.domain.AnimalRef;
import com.vetsoftware.app.laboratorytest.domain.CompanyRef;
import com.vetsoftware.app.laboratorytest.domain.ConsultationRef;
import com.vetsoftware.app.laboratorytest.domain.EmployeeRef;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestPriority;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestStatus;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestTypeRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "laboratory.test.create")
@Service
public class CreateLaboratoryTestService implements CreateLaboratoryTestUseCase {
    private final LaboratoryTestRepository repository;
    private final LaboratoryTestTypeQueryPort testTypeQueryPort;
    private final AnimalQueryPort animalQueryPort;
    private final ConsultationQueryPort consultationQueryPort;
    private final CompanyQueryPort companyQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final BranchQueryPort branchQueryPort;

    public CreateLaboratoryTestService(LaboratoryTestRepository repository,
            LaboratoryTestTypeQueryPort testTypeQueryPort, AnimalQueryPort animalQueryPort,
            ConsultationQueryPort consultationQueryPort, CompanyQueryPort companyQueryPort,
            EmployeeQueryPort employeeQueryPort, BranchQueryPort branchQueryPort) {
        this.repository = repository;
        this.testTypeQueryPort = testTypeQueryPort;
        this.animalQueryPort = animalQueryPort;
        this.consultationQueryPort = consultationQueryPort;
        this.companyQueryPort = companyQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.branchQueryPort = branchQueryPort;
    }

    @Override
    public LaboratoryTestDto execute(CreateLaboratoryTestCommand command) {
        LaboratoryTestTypeRef testType = testTypeQueryPort.findById(command.testTypeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "LaboratoryTestType not found: " + command.testTypeId()));
        AnimalRef animal = animalQueryPort.findById(command.animalId()).orElseThrow(
                () -> new IllegalArgumentException("Animal not found: " + command.animalId()));
        ConsultationRef consultation = command.consultationId() == null
                ? null
                : consultationQueryPort.findById(command.consultationId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Consultation not found: " + command.consultationId()));
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
        EmployeeRef processedBy = command.processedById() == null
                ? null
                : employeeQueryPort.findById(command.processedById())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Employee not found: " + command.processedById()));

        LaboratoryTestStatus initialStatus = command.status() == null || command.status().isBlank()
                ? LaboratoryTestStatus.PENDING_COLLECTION
                : LaboratoryTestStatus.valueOf(command.status().toUpperCase());

        LaboratoryTestPriority prioridad = command.prioridad() == null
                || command.prioridad().isBlank()
                        ? LaboratoryTestPriority.NORMAL
                        : LaboratoryTestPriority.valueOf(command.prioridad().toUpperCase());

        // Sede de la muestra: si el request trae branchId debe pertenecer a la empresa
        // y estar ACTIVA;
        // si no, la
        // sede activa por defecto. La bandeja de muestras se scopea por esta sede.
        Long branchId = command.branchId() != null
                ? branchQueryPort
                        .findActiveIdByIdAndCompanyId(command.branchId(), command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Branch is not active or not found: " + command.branchId()))
                : branchQueryPort.findDefaultActiveIdByCompanyId(command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Company has no active branch: " + command.companyId()));

        LaboratoryTest laboratoryTest = LaboratoryTest.create(command.date(), testType,
                command.quantity(), command.diagnosis(), initialStatus, prioridad, animal,
                consultation, company, branchId, processedBy, command.processedDate());
        return LaboratoryTestDto.from(repository.save(laboratoryTest));
    }
}
