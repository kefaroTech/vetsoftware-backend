package com.vetsoftware.app.baserolepermission.application.usecase;

import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import com.vetsoftware.app.baserolepermission.application.port.in.ReactivateBaseRolePermissionUseCase;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRolePermissionRepository;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "base.role.permission.reactivate")
@Service
public class ReactivateBaseRolePermissionService implements ReactivateBaseRolePermissionUseCase {
  private final BaseRolePermissionRepository repository;

  public ReactivateBaseRolePermissionService(BaseRolePermissionRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public BaseRolePermissionDto execute(Long id) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new BaseRolePermissionNotFoundException(id);
    return BaseRolePermissionDto.from(
        repository.findById(id).orElseThrow(() -> new BaseRolePermissionNotFoundException(id)));
  }
}
