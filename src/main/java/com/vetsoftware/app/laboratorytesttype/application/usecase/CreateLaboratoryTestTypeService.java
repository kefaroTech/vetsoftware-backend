package com.vetsoftware.app.laboratorytesttype.application.usecase;

import com.vetsoftware.app.laboratorytesttype.application.command.CreateLaboratoryTestTypeCommand;
import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import com.vetsoftware.app.laboratorytesttype.application.port.in.CreateLaboratoryTestTypeUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestTypeRepository;
import com.vetsoftware.app.laboratorytesttype.domain.CompanyRef;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestType;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "laboratory.test.type.create")
@Service
public class CreateLaboratoryTestTypeService implements CreateLaboratoryTestTypeUseCase {
    private final LaboratoryTestTypeRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public CreateLaboratoryTestTypeService(LaboratoryTestTypeRepository repository,
                                 CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public LaboratoryTestTypeDto execute(CreateLaboratoryTestTypeCommand command) {
        CompanyRef company = command.companyId() == null ? null
                : companyQueryPort.findById(command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        return LaboratoryTestTypeDto.from(
                repository.save(LaboratoryTestType.create(command.name(), command.description(), company, command.general())));
    }
}
