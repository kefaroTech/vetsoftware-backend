package com.vetsoftware.app.role.application.usecase;

import com.vetsoftware.app.role.application.dto.RoleDto;
import com.vetsoftware.app.role.application.port.in.ReactivateRoleUseCase;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import com.vetsoftware.app.role.domain.RoleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "role.reactivate")
@Service
public class ReactivateRoleService implements ReactivateRoleUseCase {
  private final RoleRepository repository;

  public ReactivateRoleService(RoleRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public RoleDto execute(Long id, Long companyId) {
    int rows = repository.reactivate(id, companyId);
    if (rows == 0) throw new RoleNotFoundException(id);
    return RoleDto.from(
        repository
            .findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new RoleNotFoundException(id)));
  }
}
