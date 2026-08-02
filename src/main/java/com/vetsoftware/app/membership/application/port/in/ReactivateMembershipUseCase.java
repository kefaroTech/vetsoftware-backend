package com.vetsoftware.app.membership.application.port.in;

import com.vetsoftware.app.membership.application.dto.MembershipDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateMembershipUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('membership.update')")
  MembershipDto execute(Long id);
}
