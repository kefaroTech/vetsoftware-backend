package com.vetsoftware.app.diagnosticimagingtype.application.port.out;

import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import java.util.List;
import java.util.Optional;

public interface DiagnosticImagingTypeRepository {
    DiagnosticImagingType save(DiagnosticImagingType type);

    Optional<DiagnosticImagingType> findById(Long id);

    /** Lectura: la fila propia de la empresa o cualquiera de las generales. */
    Optional<DiagnosticImagingType> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Escritura: SOLO la fila propia de la empresa. Las generales quedan fuera a
     * propósito — editarlas, borrarlas o reactivarlas las cambiaría para todos los
     * tenants.
     */
    Optional<DiagnosticImagingType> findOwnedByIdAndCompanyId(Long id, Long companyId);

    List<DiagnosticImagingType> findAll();

    List<DiagnosticImagingType> findAllAvailableForCompany(Long companyId);

    void delete(Long id);

    /**
     * Reactiva el tipo SOLO si pertenece a {@code companyId}. Devuelve las filas
     * afectadas: 0 = no existe en esa empresa.
     */
    int reactivate(Long id, Long companyId);
}
