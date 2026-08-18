package com.vetsoftware.app.employeerole.application.usecase;

import com.vetsoftware.app.employeerole.application.port.in.DeleteEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeRoleRepository;
import com.vetsoftware.app.employeerole.application.port.out.PermissionCachePort;
import com.vetsoftware.app.employeerole.domain.EmployeeRole;
import com.vetsoftware.app.employeerole.domain.EmployeeRoleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "employee.role.delete")
@Service
public class DeleteEmployeeRoleService implements DeleteEmployeeRoleUseCase {
    private final EmployeeRoleRepository repository;
    private final PermissionCachePort permissionCachePort;

    public DeleteEmployeeRoleService(EmployeeRoleRepository repository,
            PermissionCachePort permissionCachePort) {
        this.repository = repository;
        this.permissionCachePort = permissionCachePort;
    }

    /**
     * La lectura previa es la que decide si existe, asi que tiene que ir acotada:
     * cargando por id a secas, un empleado podia revocarle el rol al administrador
     * de otra empresa. {@code companyId} nulo es el principal cross-tenant
     * (SYSTEM).
     */
    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        EmployeeRole employeeRole = (companyId == null
                ? repository.findById(id)
                : repository.findByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new EmployeeRoleNotFoundException(id));
        repository.delete(id);
        permissionCachePort.evictByEmployeeId(employeeRole.getEmployee().id());
    }
}
