package com.vetsoftware.app.clinicalhistory.application.port.in;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDto;
import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GetClinicalHistoryUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('clinicalHistory.read') and @authz.isMyCompany(#query.companyId))")
  List<ClinicalEventDto> execute(GetClinicalHistoryQuery query);
}
