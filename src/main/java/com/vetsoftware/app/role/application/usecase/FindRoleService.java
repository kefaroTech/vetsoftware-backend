package com.vetsoftware.app.role.application.usecase;

import com.vetsoftware.app.role.application.dto.RoleDto;
import com.vetsoftware.app.role.application.port.in.FindRoleUseCase;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import com.vetsoftware.app.role.domain.RoleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "role.find")
@Service
public class FindRoleService implements FindRoleUseCase {
  private final RoleRepository repository;

  public FindRoleService(RoleRepository repository) {
    this.repository = repository;
  }

  @Override
  public RoleDto findById(Long id) {
    return repository
        .findById(id)
        .map(RoleDto::from)
        .orElseThrow(() -> new RoleNotFoundException(id));
  }
}
