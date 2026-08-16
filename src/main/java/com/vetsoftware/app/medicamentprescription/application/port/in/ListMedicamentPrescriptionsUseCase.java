package com.vetsoftware.app.medicamentprescription.application.port.in;

import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListMedicamentPrescriptionsUseCase {
    /**
     * Listado global de la plataforma, restringido a {@code ROLE_SYSTEM}: no filtra
     * por empresa, asi que devuelve prescripciones de todos los tenants. Por eso
     * pagina — antes traia la tabla entera de golpe.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<MedicamentPrescriptionDto> listAll(int page, int pageSize);
}
