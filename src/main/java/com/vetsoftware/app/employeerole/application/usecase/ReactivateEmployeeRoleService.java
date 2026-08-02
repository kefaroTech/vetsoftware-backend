package com.vetsoftware.app.employeerole.application.usecase;

import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import com.vetsoftware.app.employeerole.application.port.in.ReactivateEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeRoleRepository;
import com.vetsoftware.app.employeerole.application.port.out.PermissionCachePort;
import com.vetsoftware.app.employeerole.domain.EmployeeRole;
import com.vetsoftware.app.employeerole.domain.EmployeeRoleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "employee.role.reactivate")
@Service
public class ReactivateEmployeeRoleService implements ReactivateEmployeeRoleUseCase {
  private final EmployeeRoleRepository repository;
  private final PermissionCachePort permissionCachePort;

  public ReactivateEmployeeRoleService(
      EmployeeRoleRepository repository, PermissionCachePort permissionCachePort) {
    this.repository = repository;
    this.permissionCachePort = permissionCachePort;
  }

  @Override
  @Transactional
  public EmployeeRoleDto execute(Long id) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new EmployeeRoleNotFoundException(id);
    EmployeeRole employeeRole =
        repository.findById(id).orElseThrow(() -> new EmployeeRoleNotFoundException(id));
    permissionCachePort.evictByEmployeeId(employeeRole.getEmployee().id());
    return EmployeeRoleDto.from(employeeRole);
  }
}
