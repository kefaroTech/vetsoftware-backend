package com.vetsoftware.app.numberingresolution.application.port.out;

import com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolution;
import java.util.List;
import java.util.Optional;

public interface NumberingResolutionRepository {
    NumberingResolution save(NumberingResolution resolution);
    Optional<NumberingResolution> findById(Long id);
    List<NumberingResolution> findAllByCompanyId(Long companyId);
    /** Invariante "una sola resolución activa por (company, tipo)": true si ya existe una activa para ese par. */
    boolean existsActiveByCompanyAndType(Long companyId, ElectronicDocumentType documentType);
    void delete(Long id);
    int reactivate(Long id);
}
