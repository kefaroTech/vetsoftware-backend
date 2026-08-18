package com.vetsoftware.app.diagnosticimaging.application.usecase;

import com.vetsoftware.app.diagnosticimaging.application.port.in.DeleteDiagnosticImagingUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "diagnostic.imaging.delete")
@Service
public class DeleteDiagnosticImagingService implements DeleteDiagnosticImagingUseCase {
    private final DiagnosticImagingRepository repository;

    public DeleteDiagnosticImagingService(DiagnosticImagingRepository repository) {
        this.repository = repository;
    }

    /**
     * La lectura previa acotada por empresa es lo que convierte un id ajeno en un
     * 404 en vez de en un borrado. Un {@code companyId} nulo es el actor global
     * (SYSTEM), que si puede borrar cualquier fila.
     */
    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        (companyId == null
                ? repository.findById(id)
                : repository.findByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new DiagnosticImagingNotFoundException(id));
        repository.delete(id);
    }
}
