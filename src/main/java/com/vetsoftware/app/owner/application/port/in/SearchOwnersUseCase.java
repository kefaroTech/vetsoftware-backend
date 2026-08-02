package com.vetsoftware.app.owner.application.port.in;

import com.vetsoftware.app.owner.application.dto.OwnerDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SearchOwnersUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('owner.read') and @authz.isMyCompany(#companyId))")
  List<OwnerDto> search(Long companyId, String query);
}
