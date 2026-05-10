package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.ListAvailableDiagnosticImagingTypesUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "diagnostic_imaging_type.list_available")
@Service
public class ListAvailableDiagnosticImagingTypesService implements ListAvailableDiagnosticImagingTypesUseCase {
    private final DiagnosticImagingTypeRepository repository;

    public ListAvailableDiagnosticImagingTypesService(DiagnosticImagingTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<DiagnosticImagingTypeDto> listAvailable(Long companyId) {
        return repository.findAllAvailableForCompany(companyId).stream().map(DiagnosticImagingTypeDto::from).toList();
    }
}
