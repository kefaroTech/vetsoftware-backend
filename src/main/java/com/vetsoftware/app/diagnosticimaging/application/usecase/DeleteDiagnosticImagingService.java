package com.vetsoftware.app.diagnosticimaging.application.usecase;

import com.vetsoftware.app.diagnosticimaging.application.port.in.DeleteDiagnosticImagingUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "diagnostic_imaging.delete")
@Service
public class DeleteDiagnosticImagingService implements DeleteDiagnosticImagingUseCase {
    private final DiagnosticImagingRepository repository;

    public DeleteDiagnosticImagingService(DiagnosticImagingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new DiagnosticImagingNotFoundException(id));
        repository.delete(id);
    }
}
