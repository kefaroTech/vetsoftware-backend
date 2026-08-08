package com.vetsoftware.app.diagnosticimaging.application.port.out;

import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImaging;
import com.vetsoftware.app.diagnosticimaging.application.dto.PageResult;
import java.util.List;
import java.util.Optional;

public interface DiagnosticImagingRepository {
    DiagnosticImaging save(DiagnosticImaging imaging);

    Optional<DiagnosticImaging> findById(Long id);

    Optional<DiagnosticImaging> findByIdAndCompanyId(Long id, Long companyId);

    List<DiagnosticImaging> findAll();

    PageResult<DiagnosticImaging> findAllByAnimalId(Long animalId, int page, int pageSize);

    void delete(Long id);

    int reactivate(Long id);
}
