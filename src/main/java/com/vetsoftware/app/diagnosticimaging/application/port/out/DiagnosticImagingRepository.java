package com.vetsoftware.app.diagnosticimaging.application.port.out;

import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImaging;
import java.util.List;
import java.util.Optional;

public interface DiagnosticImagingRepository {
    DiagnosticImaging save(DiagnosticImaging imaging);
    Optional<DiagnosticImaging> findById(Long id);
    Optional<DiagnosticImaging> findByIdAndCompanyId(Long id, Long companyId);
    List<DiagnosticImaging> findAll();
    List<DiagnosticImaging> findAllByAnimalId(Long animalId);
    void delete(Long id);
    int reactivate(Long id);
}
