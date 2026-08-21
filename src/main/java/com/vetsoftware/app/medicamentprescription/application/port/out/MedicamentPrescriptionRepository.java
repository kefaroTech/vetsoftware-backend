package com.vetsoftware.app.medicamentprescription.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;

public interface MedicamentPrescriptionRepository {
    MedicamentPrescription save(MedicamentPrescription medicament);

    /**
     * Listado paginado. Antes era un {@code findAll()} sin filtro de empresa: la
     * tabla entera de TODOS los tenants en memoria antes de serializarla.
     */
    PageResult<MedicamentPrescription> findAll(int page, int pageSize);
}
