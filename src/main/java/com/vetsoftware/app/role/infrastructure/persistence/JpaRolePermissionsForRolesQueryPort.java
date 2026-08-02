package com.vetsoftware.app.role.infrastructure.persistence;

import com.vetsoftware.app.role.application.dto.PermissionSummaryDto;
import com.vetsoftware.app.role.application.port.out.RolePermissionsForRolesQueryPort;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class JpaRolePermissionsForRolesQueryPort implements RolePermissionsForRolesQueryPort {
  private final RolePermissionJpaRepository repository;

  public JpaRolePermissionsForRolesQueryPort(RolePermissionJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public Map<Long, List<PermissionSummaryDto>> findByRoleIds(List<Long> roleIds) {
    if (roleIds.isEmpty()) return Map.of();
    return repository.findByRoleIdIn(roleIds).stream()
        .collect(
            Collectors.groupingBy(
                rp -> rp.getRole().getId(),
                Collectors.mapping(
                    rp ->
                        new PermissionSummaryDto(
                            rp.getId(),
                            rp.getPermission().getId(),
                            rp.getPermission().getName(),
                            rp.getPermission().getCode()),
                    Collectors.toList())));
  }
}
