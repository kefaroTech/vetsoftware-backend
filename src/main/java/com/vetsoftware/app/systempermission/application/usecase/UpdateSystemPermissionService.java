package com.vetsoftware.app.systempermission.application.usecase;

import com.vetsoftware.app.systempermission.application.command.UpdateSystemPermissionCommand;
import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import com.vetsoftware.app.systempermission.application.port.in.UpdateSystemPermissionUseCase;
import com.vetsoftware.app.systempermission.application.port.out.SystemPermissionRepository;
import com.vetsoftware.app.systempermission.domain.SystemPermission;
import com.vetsoftware.app.systempermission.domain.SystemPermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "system.permission.update")
@Service
public class UpdateSystemPermissionService implements UpdateSystemPermissionUseCase {
  private final SystemPermissionRepository repository;

  public UpdateSystemPermissionService(SystemPermissionRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public SystemPermissionDto execute(UpdateSystemPermissionCommand command) {
    SystemPermission systemPermission =
        repository
            .findById(command.id())
            .orElseThrow(() -> new SystemPermissionNotFoundException(command.id()));
    systemPermission.update(command.name(), command.code());
    return SystemPermissionDto.from(repository.save(systemPermission));
  }
}
