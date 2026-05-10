package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.ListDiagnosticImagingTypesUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "diagnostic_imaging_type.list")
@Service
public class ListDiagnosticImagingTypesService implements ListDiagnosticImagingTypesUseCase {
    private final DiagnosticImagingTypeRepository repository;

    public ListDiagnosticImagingTypesService(DiagnosticImagingTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<DiagnosticImagingTypeDto> listAll() {
        return repository.findAll().stream().map(DiagnosticImagingTypeDto::from).toList();
    }
}
