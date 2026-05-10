package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import com.vetsoftware.app.diagnosticimagingtype.application.command.UpdateDiagnosticImagingTypeCommand;
import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.UpdateDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "diagnostic_imaging_type.update")
@Service
public class UpdateDiagnosticImagingTypeService implements UpdateDiagnosticImagingTypeUseCase {
    private final DiagnosticImagingTypeRepository repository;

    public UpdateDiagnosticImagingTypeService(DiagnosticImagingTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public DiagnosticImagingTypeDto execute(UpdateDiagnosticImagingTypeCommand command) {
        DiagnosticImagingType type = repository.findById(command.id())
                .orElseThrow(() -> new DiagnosticImagingTypeNotFoundException(command.id()));
        type.update(command.name(), command.description());
        return DiagnosticImagingTypeDto.from(repository.save(type));
    }
}
