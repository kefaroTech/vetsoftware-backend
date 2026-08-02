package com.vetsoftware.app.role.application.usecase;

import com.vetsoftware.app.role.application.dto.PermissionSummaryDto;
import com.vetsoftware.app.role.application.dto.RoleDto;
import com.vetsoftware.app.role.application.port.in.ListRolesByCompanyUseCase;
import com.vetsoftware.app.role.application.port.out.RolePermissionsForRolesQueryPort;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import com.vetsoftware.app.role.domain.Role;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Observed(name = "role.list.by.company")
@Service
public class ListRolesByCompanyService implements ListRolesByCompanyUseCase {
  private final RoleRepository repository;
  private final RolePermissionsForRolesQueryPort permissionsPort;

  public ListRolesByCompanyService(
      RoleRepository repository, RolePermissionsForRolesQueryPort permissionsPort) {
    this.repository = repository;
    this.permissionsPort = permissionsPort;
  }

  @Override
  public List<RoleDto> listByCompany(Long companyId) {
    List<Role> roles = repository.findAllByCompanyId(companyId);
    if (roles.isEmpty()) return List.of();
    Map<Long, List<PermissionSummaryDto>> permsByRole =
        permissionsPort.findByRoleIds(roles.stream().map(Role::getId).toList());
    return roles.stream()
        .map(r -> RoleDto.from(r, permsByRole.getOrDefault(r.getId(), List.of())))
        .toList();
  }
}
