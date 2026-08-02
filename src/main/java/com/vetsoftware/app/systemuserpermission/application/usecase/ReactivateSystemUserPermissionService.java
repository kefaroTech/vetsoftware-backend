package com.vetsoftware.app.systemuserpermission.application.usecase;

import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import com.vetsoftware.app.systemuserpermission.application.port.in.ReactivateSystemUserPermissionUseCase;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemUserPermissionRepository;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "system.user.permission.reactivate")
@Service
public class ReactivateSystemUserPermissionService
    implements ReactivateSystemUserPermissionUseCase {
  private final SystemUserPermissionRepository repository;

  public ReactivateSystemUserPermissionService(SystemUserPermissionRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public SystemUserPermissionDto execute(Long id) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new SystemUserPermissionNotFoundException(id);
    return SystemUserPermissionDto.from(
        repository.findById(id).orElseThrow(() -> new SystemUserPermissionNotFoundException(id)));
  }
}
