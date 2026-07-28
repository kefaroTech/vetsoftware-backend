package com.vetsoftware.app.diagnosticimaging.application.usecase;

import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.diagnosticimaging.application.port.in.ListDiagnosticImagingsByAnimalUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "diagnostic.imaging.list.by.animal")
@Service
public class ListDiagnosticImagingsByAnimalService implements ListDiagnosticImagingsByAnimalUseCase {
    private final DiagnosticImagingRepository repository;

    public ListDiagnosticImagingsByAnimalService(DiagnosticImagingRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<DiagnosticImagingDto> listByAnimal(Long animalId) {
        return repository.findAllByAnimalId(animalId).stream().map(DiagnosticImagingDto::from).toList();
    }
}
