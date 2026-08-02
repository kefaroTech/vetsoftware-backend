package com.vetsoftware.app.owner.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteOwnerUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('owner.delete') and @authz.isMyCompany(#companyId))")
  void execute(Long id, Long companyId);
}
