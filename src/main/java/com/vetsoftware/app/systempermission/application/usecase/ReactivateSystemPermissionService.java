package com.vetsoftware.app.systempermission.application.usecase;

import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import com.vetsoftware.app.systempermission.application.port.in.ReactivateSystemPermissionUseCase;
import com.vetsoftware.app.systempermission.application.port.out.SystemPermissionRepository;
import com.vetsoftware.app.systempermission.domain.SystemPermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "system.permission.reactivate")
@Service
public class ReactivateSystemPermissionService implements ReactivateSystemPermissionUseCase {
  private final SystemPermissionRepository repository;

  public ReactivateSystemPermissionService(SystemPermissionRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public SystemPermissionDto execute(Long id) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new SystemPermissionNotFoundException(id);
    return SystemPermissionDto.from(
        repository.findById(id).orElseThrow(() -> new SystemPermissionNotFoundException(id)));
  }
}
