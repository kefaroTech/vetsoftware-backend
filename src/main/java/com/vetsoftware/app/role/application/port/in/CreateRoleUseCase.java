package com.vetsoftware.app.role.application.port.in;

import com.vetsoftware.app.role.application.command.CreateRoleCommand;
import com.vetsoftware.app.role.application.dto.RoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateRoleUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('rolePermissions.create') and @authz.isMyCompany(#command.companyId))")
  RoleDto execute(CreateRoleCommand command);
}
