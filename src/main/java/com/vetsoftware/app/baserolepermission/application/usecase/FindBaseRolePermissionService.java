package com.vetsoftware.app.baserolepermission.application.usecase;

import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import com.vetsoftware.app.baserolepermission.application.port.in.FindBaseRolePermissionUseCase;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRolePermissionRepository;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "base.role.permission.find")
@Service
public class FindBaseRolePermissionService implements FindBaseRolePermissionUseCase {
  private final BaseRolePermissionRepository repository;

  public FindBaseRolePermissionService(BaseRolePermissionRepository repository) {
    this.repository = repository;
  }

  @Override
  public BaseRolePermissionDto findById(Long id) {
    return repository
        .findById(id)
        .map(BaseRolePermissionDto::from)
        .orElseThrow(() -> new BaseRolePermissionNotFoundException(id));
  }
}
