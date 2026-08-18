package com.vetsoftware.app.diagnosticimaging.application.usecase;

import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.diagnosticimaging.application.port.in.ReactivateDiagnosticImagingUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "diagnostic.imaging.reactivate")
@Service
public class ReactivateDiagnosticImagingService implements ReactivateDiagnosticImagingUseCase {
    private final DiagnosticImagingRepository repository;

    public ReactivateDiagnosticImagingService(DiagnosticImagingRepository repository) {
        this.repository = repository;
    }

    /**
     * La empresa viaja hasta el UPDATE y hasta la relectura: aqui no hay un
     * findById previo que valide la propiedad, asi que si la consulta no filtra por
     * empresa, un id ajeno se reactiva sin mas. Cero filas afectadas significa «no
     * existe en TU empresa», que es tambien la respuesta correcta para el registro
     * de otro tenant: un 404, sin revelar que el id existe.
     */
    @Override
    @Transactional
    public DiagnosticImagingDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0)
            throw new DiagnosticImagingNotFoundException(id);
        return DiagnosticImagingDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new DiagnosticImagingNotFoundException(id)));
    }
}
