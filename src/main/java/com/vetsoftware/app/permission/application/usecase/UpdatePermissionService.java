package com.vetsoftware.app.permission.application.usecase;

import com.vetsoftware.app.permission.application.command.UpdatePermissionCommand;
import com.vetsoftware.app.permission.application.dto.PermissionDto;
import com.vetsoftware.app.permission.application.port.in.UpdatePermissionUseCase;
import com.vetsoftware.app.permission.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.permission.domain.CompanyRef;
import com.vetsoftware.app.permission.domain.Permission;
import com.vetsoftware.app.permission.domain.PermissionNotFoundException;
import com.vetsoftware.app.permission.domain.SubModuleRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "permission.update")
@Service
public class UpdatePermissionService implements UpdatePermissionUseCase {
  private final PermissionRepository repository;
  private final CompanyQueryPort companyQueryPort;
  private final SubModuleQueryPort subModuleQueryPort;

  public UpdatePermissionService(
      PermissionRepository repository,
      CompanyQueryPort companyQueryPort,
      SubModuleQueryPort subModuleQueryPort) {
    this.repository = repository;
    this.companyQueryPort = companyQueryPort;
    this.subModuleQueryPort = subModuleQueryPort;
  }

  @Override
  @Transactional
  public PermissionDto execute(UpdatePermissionCommand command) {
    Permission permission =
        repository
            .findById(command.id())
            .orElseThrow(() -> new PermissionNotFoundException(command.id()));
    CompanyRef company =
        companyQueryPort
            .findById(command.companyId())
            .orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
    SubModuleRef subModule =
        subModuleQueryPort
            .findById(command.subModuleId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException("SubModule not found: " + command.subModuleId()));
    permission.update(command.name(), command.code(), company, subModule);
    return PermissionDto.from(repository.save(permission));
  }
}
