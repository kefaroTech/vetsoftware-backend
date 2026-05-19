package com.vetsoftware.app.diagnosticimaging.application.usecase;

import com.vetsoftware.app.diagnosticimaging.application.command.ChangeDiagnosticImagingStatusCommand;
import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.diagnosticimaging.application.port.in.ChangeDiagnosticImagingStatusUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImaging;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingNotFoundException;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingStatus;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "diagnostic_imaging.change_status")
@Service
public class ChangeDiagnosticImagingStatusService implements ChangeDiagnosticImagingStatusUseCase {
    private final DiagnosticImagingRepository repository;

    public ChangeDiagnosticImagingStatusService(DiagnosticImagingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public DiagnosticImagingDto execute(ChangeDiagnosticImagingStatusCommand command) {
        DiagnosticImaging imaging = repository.findById(command.id())
            .orElseThrow(() -> new DiagnosticImagingNotFoundException(command.id()));
        DiagnosticImagingStatus newStatus = DiagnosticImagingStatus.valueOf(command.status().toUpperCase());
        imaging.changeStatus(newStatus);
        return DiagnosticImagingDto.from(repository.save(imaging));
    }
}
