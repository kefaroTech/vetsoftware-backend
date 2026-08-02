package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.PermissionResolver;
import com.vetsoftware.app.employeerole.infrastructure.persistence.EmployeeRoleJpaRepository;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPermissionResolver implements PermissionResolver {

  private final EmployeeRoleJpaRepository employeeRoleJpaRepository;
  private final RolePermissionJpaRepository rolePermissionJpaRepository;

  public JpaPermissionResolver(
      EmployeeRoleJpaRepository employeeRoleJpaRepository,
      RolePermissionJpaRepository rolePermissionJpaRepository) {
    this.employeeRoleJpaRepository = employeeRoleJpaRepository;
    this.rolePermissionJpaRepository = rolePermissionJpaRepository;
  }

  @Cacheable(value = "employee-permissions", key = "#employeeId")
  @Override
  public Set<String> resolveFor(Long employeeId) {
    List<Long> roleIds =
        employeeRoleJpaRepository.findByEmployeeId(employeeId).stream()
            .map(e -> e.getRole().getId())
            .toList();
    if (roleIds.isEmpty()) return Set.of();
    return rolePermissionJpaRepository.findByRoleIdIn(roleIds).stream()
        .map(rp -> rp.getPermission().getCode())
        .collect(Collectors.toSet());
  }

  @CacheEvict(value = "employee-permissions", key = "#employeeId")
  public void evict(Long employeeId) {}
}
