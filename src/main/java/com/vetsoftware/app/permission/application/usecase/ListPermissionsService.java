package com.vetsoftware.app.permission.application.usecase;

import com.vetsoftware.app.permission.application.dto.PermissionDto;
import com.vetsoftware.app.permission.application.port.in.ListPermissionsUseCase;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "permission.list")
@Service
public class ListPermissionsService implements ListPermissionsUseCase {
  private final PermissionRepository repository;

  public ListPermissionsService(PermissionRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<PermissionDto> listAll() {
    return repository.findAll().stream().map(PermissionDto::from).toList();
  }
}
