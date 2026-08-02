package com.vetsoftware.app.spa.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteSpaUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or " + "(hasAuthority('spa.delete') and @authz.isMyCompany(#companyId))")
  void execute(Long id, Long companyId);
}
