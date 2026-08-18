package com.vetsoftware.app.numberingresolution.application.port.out;

import com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolution;
import java.util.List;
import java.util.Optional;

public interface NumberingResolutionRepository {
    NumberingResolution save(NumberingResolution resolution);

    Optional<NumberingResolution> findById(Long id);

    Optional<NumberingResolution> findByIdAndCompanyId(Long id, Long companyId);

    List<NumberingResolution> findAllByCompanyId(Long companyId);

    /**
     * Invariante "una sola resolución activa por (company, sede, tipo)": true si ya
     * existe una activa en ese EXACTO alcance. {@code branchId} null = alcance de
     * empresa; no null = esa sede concreta.
     */
    boolean existsActiveByCompanyBranchAndType(Long companyId, Long branchId,
            ElectronicDocumentType documentType);

    void delete(Long id);

    /**
     * Reactiva la resolución SOLO si pertenece a {@code companyId}. Devuelve las
     * filas afectadas: 0 = no existe en esa empresa.
     */
    int reactivate(Long id, Long companyId);
}
