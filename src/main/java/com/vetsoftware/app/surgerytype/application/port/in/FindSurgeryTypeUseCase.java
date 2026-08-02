package com.vetsoftware.app.surgerytype.application.port.in;

import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSurgeryTypeUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('surgery.read') and @authz.isMyCompany(#companyId))")
  SurgeryTypeDto findById(Long id, Long companyId);
}
