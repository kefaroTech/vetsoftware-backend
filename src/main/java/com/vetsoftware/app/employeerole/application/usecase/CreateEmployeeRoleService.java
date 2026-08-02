package com.vetsoftware.app.employeerole.application.usecase;

import com.vetsoftware.app.employeerole.application.command.CreateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import com.vetsoftware.app.employeerole.application.port.in.CreateEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeRoleRepository;
import com.vetsoftware.app.employeerole.application.port.out.PermissionCachePort;
import com.vetsoftware.app.employeerole.application.port.out.RoleQueryPort;
import com.vetsoftware.app.employeerole.domain.EmployeeRef;
import com.vetsoftware.app.employeerole.domain.EmployeeRole;
import com.vetsoftware.app.employeerole.domain.EmployeeRoleNotFoundException;
import com.vetsoftware.app.employeerole.domain.RoleRef;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "employee.role.create")
@Service
public class CreateEmployeeRoleService implements CreateEmployeeRoleUseCase {
    private final EmployeeRoleRepository repository;
    private final EmployeeQueryPort employeeQueryPort;
    private final RoleQueryPort roleQueryPort;
    private final PermissionCachePort permissionCachePort;

    public CreateEmployeeRoleService(EmployeeRoleRepository repository,
            EmployeeQueryPort employeeQueryPort, RoleQueryPort roleQueryPort,
            PermissionCachePort permissionCachePort) {
        this.repository = repository;
        this.employeeQueryPort = employeeQueryPort;
        this.roleQueryPort = roleQueryPort;
        this.permissionCachePort = permissionCachePort;
    }

    @Override
    @Transactional
    public EmployeeRoleDto execute(CreateEmployeeRoleCommand command) {
        EmployeeRef employee = employeeQueryPort.findById(command.employeeId()).orElseThrow(
                () -> new IllegalArgumentException("Employee not found: " + command.employeeId()));
        RoleRef role = roleQueryPort.findById(command.roleId()).orElseThrow(
                () -> new IllegalArgumentException("Role not found: " + command.roleId()));

        Optional<Long> disabledId = repository.findDisabledIdByEmployeeAndRole(command.employeeId(),
                command.roleId());
        if (disabledId.isPresent()) {
            Long id = disabledId.get();
            repository.reactivate(id);
            EmployeeRole refreshed = repository.findById(id)
                    .orElseThrow(() -> new EmployeeRoleNotFoundException(id));
            EmployeeRoleDto dto = EmployeeRoleDto.from(refreshed);
            permissionCachePort.evictByEmployeeId(command.employeeId());
            return dto;
        }

        EmployeeRole employeeRole = EmployeeRole.create(employee, role);
        EmployeeRoleDto dto = EmployeeRoleDto.from(repository.save(employeeRole));
        permissionCachePort.evictByEmployeeId(command.employeeId());
        return dto;
    }
}
