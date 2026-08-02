package com.vetsoftware.app.systemuserpermission.application.usecase;

import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import com.vetsoftware.app.systemuserpermission.application.port.in.ListSystemUserPermissionsUseCase;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemUserPermissionRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "system.user.permission.list")
@Service
public class ListSystemUserPermissionsService implements ListSystemUserPermissionsUseCase {
  private final SystemUserPermissionRepository repository;

  public ListSystemUserPermissionsService(SystemUserPermissionRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<SystemUserPermissionDto> listAll() {
    return repository.findAll().stream().map(SystemUserPermissionDto::from).toList();
  }
}
