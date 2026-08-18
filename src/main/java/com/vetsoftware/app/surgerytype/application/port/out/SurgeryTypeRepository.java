package com.vetsoftware.app.surgerytype.application.port.out;

import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import java.util.List;
import java.util.Optional;

public interface SurgeryTypeRepository {
    SurgeryType save(SurgeryType surgeryType);

    Optional<SurgeryType> findById(Long id);

    /** Lectura: la fila propia de la empresa o cualquiera de las generales. */
    Optional<SurgeryType> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Escritura: SOLO la fila propia de la empresa. Las generales quedan fuera a
     * propósito — editarlas, borrarlas o reactivarlas las cambiaría para todos los
     * tenants.
     */
    Optional<SurgeryType> findOwnedByIdAndCompanyId(Long id, Long companyId);

    List<SurgeryType> findAll();

    List<SurgeryType> findAllAvailableForCompany(Long companyId);

    void delete(Long id);

    /**
     * Reactiva el tipo SOLO si pertenece a {@code companyId}. Devuelve las filas
     * afectadas: 0 = no existe en esa empresa.
     */
    int reactivate(Long id, Long companyId);
}
