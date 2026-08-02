package com.vetsoftware.app.rolepermission.application.port.in;

import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListRolePermissionsByCompanyUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('rolePermissions.read') and @authz.isMyCompany(#companyId))")
  List<RolePermissionDto> listByCompany(Long companyId);
}
