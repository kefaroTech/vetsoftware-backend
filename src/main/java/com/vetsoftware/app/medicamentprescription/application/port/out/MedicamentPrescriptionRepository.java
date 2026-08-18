package com.vetsoftware.app.medicamentprescription.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import java.util.Optional;

public interface MedicamentPrescriptionRepository {
    MedicamentPrescription save(MedicamentPrescription medicament);

    Optional<MedicamentPrescription> findById(Long id);

    Optional<MedicamentPrescription> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Listado paginado. Antes era un {@code findAll()} sin filtro de empresa: la
     * tabla entera de TODOS los tenants en memoria antes de serializarla.
     */
    PageResult<MedicamentPrescription> findAll(int page, int pageSize);

    void delete(Long id);

    /** Sin acotar: solo el camino SYSTEM ({@code companyId == null}). */
    int reactivate(Long id);

    /** Acotado a la empresa de la receta padre. */
    int reactivate(Long id, Long companyId);
}
