package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import com.vetsoftware.app.diagnosticimagingtype.application.port.in.DeleteDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingChildrenQueryPort;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeHasActiveChildrenException;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "diagnostic_imaging_type.delete")
@Service
public class DeleteDiagnosticImagingTypeService implements DeleteDiagnosticImagingTypeUseCase {
    private final DiagnosticImagingTypeRepository repository;
    private final DiagnosticImagingChildrenQueryPort diagnosticImagingChildrenQueryPort;

    public DeleteDiagnosticImagingTypeService(
            DiagnosticImagingTypeRepository repository,
            DiagnosticImagingChildrenQueryPort diagnosticImagingChildrenQueryPort) {
        this.repository = repository;
        this.diagnosticImagingChildrenQueryPort = diagnosticImagingChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new DiagnosticImagingTypeNotFoundException(id));
        if (diagnosticImagingChildrenQueryPort.existsActiveByDiagnosticImagingTypeId(id)) {
            throw new DiagnosticImagingTypeHasActiveChildrenException(id, "diagnosticImaging");
        }
        repository.delete(id);
    }
}
