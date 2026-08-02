package com.vetsoftware.app.systemuserpermission.application.usecase;

import com.vetsoftware.app.systemuserpermission.application.command.CreateSystemUserPermissionCommand;
import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import com.vetsoftware.app.systemuserpermission.application.port.in.CreateSystemUserPermissionUseCase;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemPermissionQueryPort;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemUserPermissionRepository;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemUserQueryPort;
import com.vetsoftware.app.systemuserpermission.domain.SystemPermissionRef;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermission;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermissionNotFoundException;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserRef;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "system.user.permission.create")
@Service
public class CreateSystemUserPermissionService implements CreateSystemUserPermissionUseCase {
  private final SystemUserPermissionRepository repository;
  private final SystemUserQueryPort systemUserQueryPort;
  private final SystemPermissionQueryPort systemPermissionQueryPort;

  public CreateSystemUserPermissionService(
      SystemUserPermissionRepository repository,
      SystemUserQueryPort systemUserQueryPort,
      SystemPermissionQueryPort systemPermissionQueryPort) {
    this.repository = repository;
    this.systemUserQueryPort = systemUserQueryPort;
    this.systemPermissionQueryPort = systemPermissionQueryPort;
  }

  @Override
  @Transactional
  public SystemUserPermissionDto execute(CreateSystemUserPermissionCommand command) {
    SystemUserRef systemUser =
        systemUserQueryPort
            .findById(command.systemUserId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "SystemUser not found: " + command.systemUserId()));
    SystemPermissionRef systemPermission =
        systemPermissionQueryPort
            .findById(command.systemPermissionId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "SystemPermission not found: " + command.systemPermissionId()));

    Optional<Long> disabledId =
        repository.findDisabledIdBySystemUserAndSystemPermission(
            command.systemUserId(), command.systemPermissionId());
    if (disabledId.isPresent()) {
      Long id = disabledId.get();
      repository.reactivate(id);
      SystemUserPermission refreshed =
          repository.findById(id).orElseThrow(() -> new SystemUserPermissionNotFoundException(id));
      return SystemUserPermissionDto.from(refreshed);
    }

    SystemUserPermission systemUserPermission =
        SystemUserPermission.create(systemUser, systemPermission);
    return SystemUserPermissionDto.from(repository.save(systemUserPermission));
  }
}
