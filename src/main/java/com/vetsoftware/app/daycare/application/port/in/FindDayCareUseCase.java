package com.vetsoftware.app.daycare.application.port.in;

import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindDayCareUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('dayCare.read') and @authz.isMyCompany(#companyId))")
  DayCareDto findById(Long id, Long companyId);
}
