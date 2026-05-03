package com.vetsoftware.app.laboratorytest.application.usecase;

import com.vetsoftware.app.laboratorytest.application.command.UpdateLaboratoryTestCommand;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.port.in.UpdateLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import com.vetsoftware.app.laboratorytest.application.port.out.TestTypeQueryPort;
import com.vetsoftware.app.laboratorytest.domain.AnimalRef;
import com.vetsoftware.app.laboratorytest.domain.CompanyRef;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestNotFoundException;
import com.vetsoftware.app.laboratorytest.domain.TestTypeRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "laboratory_test.update")
@Service
public class UpdateLaboratoryTestService implements UpdateLaboratoryTestUseCase {
    private final LaboratoryTestRepository repository;
    private final TestTypeQueryPort testTypeQueryPort;
    private final AnimalQueryPort animalQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public UpdateLaboratoryTestService(LaboratoryTestRepository repository,
                                       TestTypeQueryPort testTypeQueryPort,
                                       AnimalQueryPort animalQueryPort,
                                       CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.testTypeQueryPort = testTypeQueryPort;
        this.animalQueryPort = animalQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public LaboratoryTestDto execute(UpdateLaboratoryTestCommand command) {
        LaboratoryTest laboratoryTest = repository.findById(command.id())
            .orElseThrow(() -> new LaboratoryTestNotFoundException(command.id()));
        TestTypeRef testType = testTypeQueryPort.findById(command.testTypeId())
            .orElseThrow(() -> new IllegalArgumentException("TestType not found: " + command.testTypeId()));
        AnimalRef animal = animalQueryPort.findById(command.animalId())
            .orElseThrow(() -> new IllegalArgumentException("Animal not found: " + command.animalId()));
        CompanyRef company = companyQueryPort.findById(command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));

        laboratoryTest.update(command.date(), testType, command.quantity(), command.diagnosis(),
            animal, company);
        return LaboratoryTestDto.from(repository.save(laboratoryTest));
    }
}
