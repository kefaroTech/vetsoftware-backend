package com.vetsoftware.app.laboratorytesttype.application.port.out;

import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestType;
import java.util.List;
import java.util.Optional;

public interface LaboratoryTestTypeRepository {
    LaboratoryTestType save(LaboratoryTestType laboratoryTestType);

    Optional<LaboratoryTestType> findById(Long id);

    /** Lectura: la fila propia de la empresa o cualquiera de las generales. */
    Optional<LaboratoryTestType> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Escritura: SOLO la fila propia de la empresa. Las generales quedan fuera a
     * propósito — editarlas, borrarlas o reactivarlas las cambiaría para todos los
     * tenants.
     */
    Optional<LaboratoryTestType> findOwnedByIdAndCompanyId(Long id, Long companyId);

    List<LaboratoryTestType> findAll();

    List<LaboratoryTestType> findAllAvailableForCompany(Long companyId);

    void delete(Long id);

    /**
     * Reactiva el tipo SOLO si pertenece a {@code companyId}. Devuelve las filas
     * afectadas: 0 = no existe en esa empresa.
     */
    int reactivate(Long id, Long companyId);
}
