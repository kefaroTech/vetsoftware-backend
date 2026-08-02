package com.vetsoftware.app.service.application.port.in;

import com.vetsoftware.app.service.application.dto.ServiceDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindServiceUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or " + "(hasAuthority('service.read') and @authz.isMyCompany(#companyId))")
  ServiceDto findById(Long id, Long companyId);
}
