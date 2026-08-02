package com.vetsoftware.app.animal.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteWeightRecordUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('animal.delete') and @authz.isMyCompany(#companyId))")
  void execute(Long id, Long animalId, Long companyId);
}
