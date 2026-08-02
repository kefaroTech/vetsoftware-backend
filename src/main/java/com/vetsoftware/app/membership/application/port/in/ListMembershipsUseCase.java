package com.vetsoftware.app.membership.application.port.in;

import com.vetsoftware.app.membership.application.dto.MembershipDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListMembershipsUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  List<MembershipDto> listAll();
}
