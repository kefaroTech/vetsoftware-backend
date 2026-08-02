package com.vetsoftware.app.rolepermission.application.usecase;

import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import com.vetsoftware.app.rolepermission.application.port.in.ReactivateRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.out.PermissionCachePort;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository;
import com.vetsoftware.app.rolepermission.domain.RolePermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "role.permission.reactivate")
@Service
public class ReactivateRolePermissionService implements ReactivateRolePermissionUseCase {
  private final RolePermissionRepository repository;
  private final PermissionCachePort permissionCachePort;

  public ReactivateRolePermissionService(
      RolePermissionRepository repository, PermissionCachePort permissionCachePort) {
    this.repository = repository;
    this.permissionCachePort = permissionCachePort;
  }

  @Override
  @Transactional
  public RolePermissionDto execute(Long id, Long companyId) {
    int rows = companyId == null ? repository.reactivate(id) : repository.reactivate(id, companyId);
    if (rows == 0) throw new RolePermissionNotFoundException(id);
    var rolePermission =
        (companyId == null
                ? repository.findById(id)
                : repository.findByIdAndCompanyId(id, companyId))
            .orElseThrow(() -> new RolePermissionNotFoundException(id));
    permissionCachePort.evictByRoleId(rolePermission.getRole().id());
    return RolePermissionDto.from(rolePermission);
  }
}
