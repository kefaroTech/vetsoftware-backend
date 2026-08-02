package com.vetsoftware.app.withholdingconfig.application.port.in;

import com.vetsoftware.app.withholdingconfig.application.dto.WithholdingConfigDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindWithholdingConfigUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('electronicbilling.read') and"
          + " @authz.isMyCompany(#companyId))")
  WithholdingConfigDto findByCompany(Long companyId);
}
