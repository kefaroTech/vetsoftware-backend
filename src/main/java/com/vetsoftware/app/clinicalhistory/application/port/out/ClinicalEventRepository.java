package com.vetsoftware.app.clinicalhistory.application.port.out;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventTypeCountDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import com.vetsoftware.app.clinicalhistory.application.query.ListCompanyClinicalEventsQuery;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEvent;
import java.util.List;

public interface ClinicalEventRepository {
    /**
     * Historia completa del animal, sin paginar. Solo para la exportacion a PDF,
     * que por definicion es el expediente entero; la pantalla usa
     * {@link #findHistoryPage}.
     */
    List<ClinicalEvent> findHistory(GetClinicalHistoryQuery query);

    /**
     * BE-06: la pantalla lee la historia por paginas. Los filtros (tipos, rango de
     * fechas) siguen resolviendose en SQL, que es lo que permite paginar sin
     * ocultar eventos.
     */
    PageResult<ClinicalEvent> findHistoryPage(GetClinicalHistoryQuery query, int page,
            int pageSize);

    /**
     * Contador por tipo sobre toda la historia del animal, para los chips de la
     * pantalla. Ver {@link ClinicalEventTypeCountDto}.
     */
    List<ClinicalEventTypeCountDto> countByType(Long animalId, Long companyId);

    List<ClinicalEvent> findByCompany(ListCompanyClinicalEventsQuery query);
}
