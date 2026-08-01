package com.vetsoftware.app.clinicalhistory.application.port.in;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDto;
import com.vetsoftware.app.clinicalhistory.application.query.ListCompanyClinicalEventsQuery;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListCompanyClinicalEventsUseCase {
    @PreAuthorize("hasRole('SYSTEM') or @authz.isMyCompany(#query.companyId)")
    List<ClinicalEventDto> execute(ListCompanyClinicalEventsQuery query);
}
