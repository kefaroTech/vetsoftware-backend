package com.vetsoftware.app.role.application.usecase;

import com.vetsoftware.app.role.application.port.in.DeleteRoleUseCase;
import com.vetsoftware.app.role.application.port.out.EmployeeRoleChildrenQueryPort;
import com.vetsoftware.app.role.application.port.out.RolePermissionChildrenQueryPort;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import com.vetsoftware.app.role.domain.RoleHasActiveChildrenException;
import com.vetsoftware.app.role.domain.RoleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "role.delete")
@Service
public class DeleteRoleService implements DeleteRoleUseCase {
    private final RoleRepository repository;
    private final RolePermissionChildrenQueryPort rolePermissionChildrenQueryPort;
    private final EmployeeRoleChildrenQueryPort employeeRoleChildrenQueryPort;

    public DeleteRoleService(
            RoleRepository repository,
            RolePermissionChildrenQueryPort rolePermissionChildrenQueryPort,
            EmployeeRoleChildrenQueryPort employeeRoleChildrenQueryPort) {
        this.repository = repository;
        this.rolePermissionChildrenQueryPort = rolePermissionChildrenQueryPort;
        this.employeeRoleChildrenQueryPort = employeeRoleChildrenQueryPort;
    }

    @Override
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new RoleNotFoundException(id));
        if (rolePermissionChildrenQueryPort.existsActiveByRoleId(id)) {
            throw new RoleHasActiveChildrenException(id, "rolePermission");
        }
        if (employeeRoleChildrenQueryPort.existsActiveByRoleId(id)) {
            throw new RoleHasActiveChildrenException(id, "employeeRole");
        }
        repository.delete(id);
    }
}
