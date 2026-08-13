package com.vetsoftware.app.clinicalhistory.application.port.in;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventTypeCountDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GetClinicalHistorySummaryUseCase {
    /**
     * BE-06: cuantos eventos de cada tipo tiene el animal. Los chips de la pantalla
     * lo contaban sobre el array completo, que con paginacion seria solo lo
     * cargado.
     */
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('clinicalHistory.read') and @authz.isMyCompany(#companyId))")
    List<ClinicalEventTypeCountDto> countByType(Long animalId, Long companyId);
}
