package com.vetsoftware.app.permission.application.port.in;

import com.vetsoftware.app.permission.application.dto.PermissionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListPermissionsByCompanyUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('rolePermissions.read') and @authz.isMyCompany(#companyId))")
  List<PermissionDto> listByCompany(Long companyId);
}
