package com.vetsoftware.app.clinicalhistory.application.port.in;

import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ExportClinicalHistoryUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('clinicalHistory.read') and @authz.isMyCompany(#query.companyId))")
  byte[] execute(GetClinicalHistoryQuery query);
}
