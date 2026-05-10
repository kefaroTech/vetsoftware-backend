package com.vetsoftware.app.laboratorytesttype.application.usecase;

import com.vetsoftware.app.laboratorytesttype.application.command.UpdateLaboratoryTestTypeCommand;
import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import com.vetsoftware.app.laboratorytesttype.application.port.in.UpdateLaboratoryTestTypeUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestTypeRepository;
import com.vetsoftware.app.laboratorytesttype.domain.CompanyRef;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestType;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "laboratory_test_type.update")
@Service
public class UpdateLaboratoryTestTypeService implements UpdateLaboratoryTestTypeUseCase {
    private final LaboratoryTestTypeRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public UpdateLaboratoryTestTypeService(LaboratoryTestTypeRepository repository,
                                 CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public LaboratoryTestTypeDto execute(UpdateLaboratoryTestTypeCommand command) {
        LaboratoryTestType laboratoryTestType = repository.findById(command.id())
                .orElseThrow(() -> new LaboratoryTestTypeNotFoundException(command.id()));
        CompanyRef company = command.companyId() == null ? null
                : companyQueryPort.findById(command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        laboratoryTestType.update(command.name(), command.description(), company, command.general());
        return LaboratoryTestTypeDto.from(repository.save(laboratoryTestType));
    }
}
