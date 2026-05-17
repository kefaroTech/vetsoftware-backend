package com.vetsoftware.app.employeerole.application.usecase;

import com.vetsoftware.app.employeerole.application.port.in.DeleteEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeRoleRepository;
import com.vetsoftware.app.employeerole.application.port.out.PermissionCachePort;
import com.vetsoftware.app.employeerole.domain.EmployeeRole;
import com.vetsoftware.app.employeerole.domain.EmployeeRoleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "employeerole.delete")
@Service
public class DeleteEmployeeRoleService implements DeleteEmployeeRoleUseCase {
    private final EmployeeRoleRepository repository;
    private final PermissionCachePort permissionCachePort;

    public DeleteEmployeeRoleService(EmployeeRoleRepository repository,
                                     PermissionCachePort permissionCachePort) {
        this.repository = repository;
        this.permissionCachePort = permissionCachePort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        EmployeeRole employeeRole = repository.findById(id)
            .orElseThrow(() -> new EmployeeRoleNotFoundException(id));
        repository.delete(id);
        permissionCachePort.evictByEmployeeId(employeeRole.getEmployee().id());
    }
}
