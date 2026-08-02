package com.vetsoftware.app.spa.application.port.in;

import com.vetsoftware.app.spa.application.dto.SpaDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSpaUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('spa.read') and @authz.isMyCompany(#companyId))")
  SpaDto findById(Long id, Long companyId);
}
