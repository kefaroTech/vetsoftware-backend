package com.vetsoftware.app.diagnosticimaging.application.usecase;

import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.diagnosticimaging.application.dto.PageResult;
import com.vetsoftware.app.diagnosticimaging.application.port.in.ListDiagnosticImagingsByAnimalUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "diagnostic.imaging.list.by.animal")
@Service
public class ListDiagnosticImagingsByAnimalService
        implements
            ListDiagnosticImagingsByAnimalUseCase {
    private final DiagnosticImagingRepository repository;

    public ListDiagnosticImagingsByAnimalService(DiagnosticImagingRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<DiagnosticImagingDto> listByAnimal(Long animalId, Long companyId,
            String query, int page, int pageSize) {
        return repository.findAllByAnimalIdAndCompanyId(animalId, companyId, query, page, pageSize)
                .map(DiagnosticImagingDto::from);
    }
}
