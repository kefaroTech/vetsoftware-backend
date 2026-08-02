package com.vetsoftware.app.role.application.usecase;

import com.vetsoftware.app.role.application.port.in.DeleteRoleUseCase;
import com.vetsoftware.app.role.application.port.out.EmployeeRoleChildrenQueryPort;
import com.vetsoftware.app.role.application.port.out.RolePermissionChildrenCascadePort;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import com.vetsoftware.app.role.domain.RoleHasActiveChildrenException;
import com.vetsoftware.app.role.domain.RoleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "role.delete")
@Service
public class DeleteRoleService implements DeleteRoleUseCase {
    private final RoleRepository repository;
    private final EmployeeRoleChildrenQueryPort employeeRoleChildrenQueryPort;
    private final RolePermissionChildrenCascadePort rolePermissionChildrenCascadePort;

    public DeleteRoleService(RoleRepository repository,
            EmployeeRoleChildrenQueryPort employeeRoleChildrenQueryPort,
            RolePermissionChildrenCascadePort rolePermissionChildrenCascadePort) {
        this.repository = repository;
        this.employeeRoleChildrenQueryPort = employeeRoleChildrenQueryPort;
        this.rolePermissionChildrenCascadePort = rolePermissionChildrenCascadePort;
    }

    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new RoleNotFoundException(id));
        if (employeeRoleChildrenQueryPort.existsActiveByRoleId(id)) {
            throw new RoleHasActiveChildrenException(id, "employeeRole");
        }
        rolePermissionChildrenCascadePort.deactivateAllByRoleId(id);
        repository.delete(id);
    }
}
