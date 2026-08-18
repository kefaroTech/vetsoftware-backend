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

    /**
     * La empresa viaja hasta el UPDATE y hasta la relectura: aquí no hay un
     * findById previo que valide la propiedad, así que sin filtrar por empresa se
     * revivía el tipo de otro tenant. La relectura usa el finder ESTRICTO —lo
     * reactivado es siempre propio— y no el de disponibles, que incluye las
     * generales.
     */
    @Override
    @Transactional
    public DiagnosticImagingTypeDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0)
            throw new DiagnosticImagingTypeNotFoundException(id);
        return DiagnosticImagingTypeDto.from(repository.findOwnedByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new DiagnosticImagingTypeNotFoundException(id)));
    }
}
