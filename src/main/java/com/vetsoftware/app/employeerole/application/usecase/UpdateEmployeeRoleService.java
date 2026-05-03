package com.vetsoftware.app.employeerole.application.usecase;

import com.vetsoftware.app.employeerole.application.command.UpdateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import com.vetsoftware.app.employeerole.application.port.in.UpdateEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeRoleRepository;
import com.vetsoftware.app.employeerole.application.port.out.RoleQueryPort;
import com.vetsoftware.app.employeerole.domain.EmployeeRef;
import com.vetsoftware.app.employeerole.domain.EmployeeRole;
import com.vetsoftware.app.employeerole.domain.EmployeeRoleNotFoundException;
import com.vetsoftware.app.employeerole.domain.RoleRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "employeerole.update")
@Service
public class UpdateEmployeeRoleService implements UpdateEmployeeRoleUseCase {
    private final EmployeeRoleRepository repository;
    private final EmployeeQueryPort employeeQueryPort;
    private final RoleQueryPort roleQueryPort;

    public UpdateEmployeeRoleService(EmployeeRoleRepository repository,
                                     EmployeeQueryPort employeeQueryPort,
                                     RoleQueryPort roleQueryPort) {
        this.repository = repository;
        this.employeeQueryPort = employeeQueryPort;
        this.roleQueryPort = roleQueryPort;
    }

    @Override
    @Transactional
    public EmployeeRoleDto execute(UpdateEmployeeRoleCommand command) {
        EmployeeRole employeeRole = repository.findById(command.id())
            .orElseThrow(() -> new EmployeeRoleNotFoundException(command.id()));
        EmployeeRef employee = employeeQueryPort.findById(command.employeeId())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.employeeId()));
        RoleRef role = roleQueryPort.findById(command.roleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + command.roleId()));
        employeeRole.update(employee, role);
        return EmployeeRoleDto.from(repository.save(employeeRole));
    }
}
