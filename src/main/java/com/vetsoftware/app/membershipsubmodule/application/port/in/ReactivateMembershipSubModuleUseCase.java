package com.vetsoftware.app.membershipsubmodule.application.port.in;

import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateMembershipSubModuleUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('membership_sub_module.update')")
  MembershipSubModuleDto execute(Long id);
}
