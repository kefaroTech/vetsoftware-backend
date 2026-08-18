package com.vetsoftware.app.vaccinationtype.application.port.out;

import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import java.util.List;
import java.util.Optional;

public interface VaccinationTypeRepository {
    VaccinationType save(VaccinationType vaccinationType);

    Optional<VaccinationType> findById(Long id);

    /** Lectura: la fila propia de la empresa o cualquiera de las generales. */
    Optional<VaccinationType> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Escritura: SOLO la fila propia de la empresa. Las generales quedan fuera a
     * propósito — editarlas, borrarlas o reactivarlas las cambiaría para todos los
     * tenants.
     */
    Optional<VaccinationType> findOwnedByIdAndCompanyId(Long id, Long companyId);

    List<VaccinationType> findAll();

    List<VaccinationType> findAllAvailableForCompany(Long companyId);

    void delete(Long id);

    /**
     * Reactiva el tipo SOLO si pertenece a {@code companyId}. Devuelve las filas
     * afectadas: 0 = no existe en esa empresa.
     */
    int reactivate(Long id, Long companyId);
}
