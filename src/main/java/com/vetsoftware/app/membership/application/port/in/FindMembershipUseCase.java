package com.vetsoftware.app.membership.application.port.in;

import com.vetsoftware.app.membership.application.dto.MembershipDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindMembershipUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  MembershipDto findById(Long id);
}
