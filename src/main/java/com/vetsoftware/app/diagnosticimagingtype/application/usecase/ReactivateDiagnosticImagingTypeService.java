package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.ReactivateDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "diagnostic.imaging.type.reactivate")
@Service
public class ReactivateDiagnosticImagingTypeService
        implements
            ReactivateDiagnosticImagingTypeUseCase {
    private final DiagnosticImagingTypeRepository repository;

    public ReactivateDiagnosticImagingTypeService(DiagnosticImagingTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public DiagnosticImagingTypeDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0)
            throw new DiagnosticImagingTypeNotFoundException(id);
        return DiagnosticImagingTypeDto.from(repository.findById(id)
                .orElseThrow(() -> new DiagnosticImagingTypeNotFoundException(id)));
    }
}
