package com.vetsoftware.app.role.application.port.in;

import com.vetsoftware.app.role.application.dto.RoleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListRolesUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('rolePermissions.read')")
  List<RoleDto> listAll();
}
