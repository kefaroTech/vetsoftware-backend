package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.FindDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "diagnostic.imaging.type.find")
@Service
public class FindDiagnosticImagingTypeService implements FindDiagnosticImagingTypeUseCase {
    private final DiagnosticImagingTypeRepository repository;

    public FindDiagnosticImagingTypeService(DiagnosticImagingTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public DiagnosticImagingTypeDto findById(Long id) {
        return DiagnosticImagingTypeDto.from(repository.findById(id)
                .orElseThrow(() -> new DiagnosticImagingTypeNotFoundException(id)));
    }
}
