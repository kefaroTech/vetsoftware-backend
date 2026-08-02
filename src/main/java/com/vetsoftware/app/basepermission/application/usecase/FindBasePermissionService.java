package com.vetsoftware.app.basepermission.application.usecase;

import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import com.vetsoftware.app.basepermission.application.port.in.FindBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.out.BasePermissionRepository;
import com.vetsoftware.app.basepermission.domain.BasePermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "base.permission.find")
@Service
public class FindBasePermissionService implements FindBasePermissionUseCase {
  private final BasePermissionRepository repository;

  public FindBasePermissionService(BasePermissionRepository repository) {
    this.repository = repository;
  }

  @Override
  public BasePermissionDto findById(Long id) {
    return repository
        .findById(id)
        .map(BasePermissionDto::from)
        .orElseThrow(() -> new BasePermissionNotFoundException(id));
  }
}
