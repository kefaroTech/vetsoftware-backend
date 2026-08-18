package com.vetsoftware.app.laboratorytest.application.usecase;

import com.vetsoftware.app.laboratorytest.application.command.UpdateLaboratoryTestCommand;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.port.in.UpdateLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.application.port.out.AnimalQueryPort;
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
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestNotFoundException;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestPriority;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestTypeRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "laboratory.test.update")
@Service
public class UpdateLaboratoryTestService implements UpdateLaboratoryTestUseCase {
    private final LaboratoryTestRepository repository;
    private final LaboratoryTestTypeQueryPort testTypeQueryPort;
    private final AnimalQueryPort animalQueryPort;
    private final ConsultationQueryPort consultationQueryPort;
    private final CompanyQueryPort companyQueryPort;
    private final EmployeeQueryPort employeeQueryPort;

    public UpdateLaboratoryTestService(LaboratoryTestRepository repository,
            LaboratoryTestTypeQueryPort testTypeQueryPort, AnimalQueryPort animalQueryPort,
            ConsultationQueryPort consultationQueryPort, CompanyQueryPort companyQueryPort,
            EmployeeQueryPort employeeQueryPort) {
        this.repository = repository;
        this.testTypeQueryPort = testTypeQueryPort;
        this.animalQueryPort = animalQueryPort;
        this.consultationQueryPort = consultationQueryPort;
        this.companyQueryPort = companyQueryPort;
        this.employeeQueryPort = employeeQueryPort;
    }

    @Override
    @Transactional
    public LaboratoryTestDto execute(UpdateLaboratoryTestCommand command) {
        // Sin acotar por empresa, el @authz.isMyCompany(#command.companyId) del puerto
        // es vacuo: solo prueba que el atacante declara SU empresa, y el update
        // posterior reescribe el company de la fila ajena — apropiacion, no rechazo.
        LaboratoryTest laboratoryTest = (command.companyId() == null
                ? repository.findById(command.id())
                : repository.findByIdAndCompanyId(command.id(), command.companyId()))
                .orElseThrow(() -> new LaboratoryTestNotFoundException(command.id()));
        Long companyId = command.companyId() == null
                ? laboratoryTest.getCompany().id()
                : command.companyId();
        // Las referencias entrantes se resuelven acotadas por la MISMA empresa que la
        // fila. Sin eso el update ya no se apropia de nada ajeno, pero si cuelga lo
        // propio de un padre de otro tenant: una orden de laboratorio de mi empresa en
        // la historia clinica de la vecina. El tipo va por la variante «general O mia»,
        // porque ese catalogo mezcla filas globales con las privadas de cada empresa.
        LaboratoryTestTypeRef testType = testTypeQueryPort
                .findAvailableByIdAndCompanyId(command.testTypeId(), companyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "LaboratoryTestType not found: " + command.testTypeId()));
        AnimalRef animal = animalQueryPort.findByIdAndCompanyId(command.animalId(), companyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal not found: " + command.animalId()));
        ConsultationRef consultation = command.consultationId() == null
                ? null
                : consultationQueryPort.findByIdAndCompanyId(command.consultationId(), companyId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Consultation not found: " + command.consultationId()));
        CompanyRef company = companyQueryPort.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
        EmployeeRef processedBy = command.processedById() == null
                ? null
                : employeeQueryPort.findById(command.processedById())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Employee not found: " + command.processedById()));

        LaboratoryTestPriority prioridad = command.prioridad() == null
                || command.prioridad().isBlank()
                        ? laboratoryTest.getPrioridad()
                        : LaboratoryTestPriority.valueOf(command.prioridad().toUpperCase());

        laboratoryTest.update(command.date(), testType, command.quantity(), command.diagnosis(),
                prioridad, animal, consultation, company, processedBy, command.processedDate());
        return LaboratoryTestDto.from(repository.save(laboratoryTest));
    }
}
