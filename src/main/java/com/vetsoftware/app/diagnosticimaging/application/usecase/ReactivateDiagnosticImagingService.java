package com.vetsoftware.app.diagnosticimaging.application.usecase;

import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.diagnosticimaging.application.port.in.ReactivateDiagnosticImagingUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "diagnosticimaging.reactivate")
@Service
public class ReactivateDiagnosticImagingService implements ReactivateDiagnosticImagingUseCase {
    private final DiagnosticImagingRepository repository;

    public ReactivateDiagnosticImagingService(DiagnosticImagingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public DiagnosticImagingDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new DiagnosticImagingNotFoundException(id);
        return DiagnosticImagingDto.from(repository.findById(id)
            .orElseThrow(() -> new DiagnosticImagingNotFoundException(id)));
    }
}
