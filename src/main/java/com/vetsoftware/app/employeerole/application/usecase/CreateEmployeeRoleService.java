package com.vetsoftware.app.employeerole.application.usecase;

import com.vetsoftware.app.employeerole.application.command.CreateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import com.vetsoftware.app.employeerole.application.port.in.CreateEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeRoleRepository;
import com.vetsoftware.app.employeerole.application.port.out.RoleQueryPort;
import com.vetsoftware.app.employeerole.domain.EmployeeRef;
import com.vetsoftware.app.employeerole.domain.EmployeeRole;
import com.vetsoftware.app.employeerole.domain.RoleRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "employeerole.create")
@Service
public class CreateEmployeeRoleService implements CreateEmployeeRoleUseCase {
    private final EmployeeRoleRepository repository;
    private final EmployeeQueryPort employeeQueryPort;
    private final RoleQueryPort roleQueryPort;

    public CreateEmployeeRoleService(EmployeeRoleRepository repository,
                                     EmployeeQueryPort employeeQueryPort,
                                     RoleQueryPort roleQueryPort) {
        this.repository = repository;
        this.employeeQueryPort = employeeQueryPort;
        this.roleQueryPort = roleQueryPort;
    }

    @Override
    public EmployeeRoleDto execute(CreateEmployeeRoleCommand command) {
        EmployeeRef employee = employeeQueryPort.findById(command.employeeId())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.employeeId()));
        RoleRef role = roleQueryPort.findById(command.roleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + command.roleId()));
        EmployeeRole employeeRole = EmployeeRole.create(employee, role);
        return EmployeeRoleDto.from(repository.save(employeeRole));
    }
}
