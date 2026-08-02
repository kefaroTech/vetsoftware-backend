package com.vetsoftware.app.role.application.port.in;

import com.vetsoftware.app.role.application.dto.RoleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListRolesByCompanyUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or ((hasAuthority('rolePermissions.read') or hasAuthority('role.read'))"
          + " and @authz.isMyCompany(#companyId))")
  List<RoleDto> listByCompany(Long companyId);
}
