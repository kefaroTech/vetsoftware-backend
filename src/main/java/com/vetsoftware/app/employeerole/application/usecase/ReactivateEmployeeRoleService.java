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

    public ReactivateEmployeeRoleService(EmployeeRoleRepository repository,
            PermissionCachePort permissionCachePort) {
        this.repository = repository;
        this.permissionCachePort = permissionCachePort;
    }

    /**
     * La empresa viaja hasta el UPDATE y hasta la relectura: aqui no hay un
     * findById previo que valide la propiedad, asi que si la consulta no filtra por
     * empresa, un id ajeno se reactiva sin mas. Y lo que se reactiva no es un dato
     * de negocio sino un privilegio: devolver una asignacion revocada le regresa al
     * empleado permisos que su propio administrador le quito, y el evict de la
     * cache lo hace efectivo en el acto. Cero filas afectadas significa «no existe
     * en TU empresa», que es tambien la respuesta correcta para la asignacion de
     * otro tenant: un 404, sin revelar que el id existe.
     *
     * <p>
     * {@code companyId} nulo es el principal cross-tenant (SYSTEM), que si opera
     * global.
     */
    @Override
    @Transactional
    public EmployeeRoleDto execute(Long id, Long companyId) {
        int rows = companyId == null
                ? repository.reactivate(id)
                : repository.reactivate(id, companyId);
        if (rows == 0)
            throw new EmployeeRoleNotFoundException(id);
        EmployeeRole employeeRole = (companyId == null
                ? repository.findById(id)
                : repository.findByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new EmployeeRoleNotFoundException(id));
        permissionCachePort.evictByEmployeeId(employeeRole.getEmployee().id());
        return EmployeeRoleDto.from(employeeRole);
    }
}
