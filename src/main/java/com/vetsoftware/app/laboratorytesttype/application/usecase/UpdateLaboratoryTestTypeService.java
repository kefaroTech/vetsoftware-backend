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

@Observed(name = "laboratory.test.type.update")
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
        // El @PreAuthorize solo prueba que el caller declara SU empresa; sin acotar
        // aqui la lectura, un id ajeno se cargaba y el update posterior reescribia su
        // company: el tipo de otro tenant pasaba a tener el company_id del atacante.
        // Finder ESTRICTO a proposito: una fila general tampoco se puede apropiar.
        LaboratoryTestType laboratoryTestType = (command.companyId() == null
                ? repository.findById(command.id())
                : repository.findOwnedByIdAndCompanyId(command.id(), command.companyId()))
                .orElseThrow(() -> new LaboratoryTestTypeNotFoundException(command.id()));
        CompanyRef company = command.companyId() == null
                ? null
                : companyQueryPort.findById(command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Company not found: " + command.companyId()));
        laboratoryTestType.update(command.name(), command.description(), company,
                command.general());
        return LaboratoryTestTypeDto.from(repository.save(laboratoryTestType));
    }
}
