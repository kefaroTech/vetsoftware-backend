package com.vetsoftware.app.membership.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteMembershipUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  void execute(Long id);
}
