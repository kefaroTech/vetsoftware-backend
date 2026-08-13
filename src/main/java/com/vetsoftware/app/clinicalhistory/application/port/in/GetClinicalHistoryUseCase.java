package com.vetsoftware.app.clinicalhistory.application.port.in;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDto;
import com.vetsoftware.app.clinicalhistory.application.dto.PageResult;
import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GetClinicalHistoryUseCase {
    /**
     * BE-06: devuelve una pagina, no la historia entera. Un paciente cronico
     * acumula cientos de eventos y la pantalla solo pinta los primeros.
     */
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('clinicalHistory.read') and @authz.isMyCompany(#query.companyId))")
    PageResult<ClinicalEventDto> execute(GetClinicalHistoryQuery query, int page, int pageSize);
}
