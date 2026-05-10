package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import com.vetsoftware.app.diagnosticimagingtype.application.command.CreateDiagnosticImagingTypeCommand;
import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.CreateDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "diagnostic_imaging_type.create")
@Service
public class CreateDiagnosticImagingTypeService implements CreateDiagnosticImagingTypeUseCase {
    private final DiagnosticImagingTypeRepository repository;

    public CreateDiagnosticImagingTypeService(DiagnosticImagingTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public DiagnosticImagingTypeDto execute(CreateDiagnosticImagingTypeCommand command) {
        return DiagnosticImagingTypeDto.from(
                repository.save(DiagnosticImagingType.create(command.name(), command.description())));
    }
}
