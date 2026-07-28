package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import com.vetsoftware.app.diagnosticimagingtype.application.command.CreateDiagnosticImagingTypeCommand;
import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.CreateDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.CompanyRef;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "diagnostic.imaging.type.create")
@Service
public class CreateDiagnosticImagingTypeService implements CreateDiagnosticImagingTypeUseCase {
    private final DiagnosticImagingTypeRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public CreateDiagnosticImagingTypeService(DiagnosticImagingTypeRepository repository,
                                              CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public DiagnosticImagingTypeDto execute(CreateDiagnosticImagingTypeCommand command) {
        CompanyRef company = command.companyId() == null ? null
                : companyQueryPort.findById(command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        return DiagnosticImagingTypeDto.from(
                repository.save(DiagnosticImagingType.create(command.name(), command.description(), company, command.general())));
    }
}
