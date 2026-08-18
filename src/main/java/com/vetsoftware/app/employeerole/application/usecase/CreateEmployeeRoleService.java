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

    /**
     * Aqui la empresa acota las <b>referencias entrantes</b>, no la carga de la
     * fila propia: en un alta no hay fila propia todavia. Con
     * {@code employeeQueryPort.findById(command.employeeId())} a secas, un
     * administrador de A con {@code employee.create} le asignaba un rol a un
     * empleado de B adivinando su id, y el {@code evictByEmployeeId} de mas abajo
     * lo hacia efectivo en el acto: escalada de privilegios cross-tenant por la via
     * de la referencia. El {@code @authz.isMyCompany} del puerto no lo veia —solo
     * prueba que el caller declara su propia empresa, no de quien es el empleado—.
     *
     * <p>
     * Las <b>dos</b> referencias van acotadas. El rol tambien es de una empresa
     * ({@code roles.company_id}, unique {@code (company_id, code)}): acotar solo el
     * empleado dejaba colgar el rol de B de un empleado propio, que es entregarle
     * los permisos que la membresia de A no autoriza.
     *
     * <p>
     * La reactivacion de la fila desactivada tambien viaja acotada. Ahi no hay
     * lectura previa que valide la propiedad, asi que el {@code AND company_id} del
     * UPDATE es la unica barrera; cero filas afectadas deja la relectura vacia y
     * sale {@link EmployeeRoleNotFoundException} —un 404, sin revelar que el id
     * existe—.
     *
     * <p>
     * {@code companyId} nulo es el principal cross-tenant (SYSTEM), que si opera
     * global: es el camino del registro de una empresa nueva, que se auto-asigna el
     * rol ADMIN cuando todavia no hay ningun empleado con sesion.
     */
    @Override
    @Transactional
    public EmployeeRoleDto execute(CreateEmployeeRoleCommand command) {
        Long companyId = command.companyId();
        EmployeeRef employee = (companyId == null
                ? employeeQueryPort.findById(command.employeeId())
                : employeeQueryPort.findByIdAndCompanyId(command.employeeId(), companyId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employee not found: " + command.employeeId()));
        RoleRef role = (companyId == null
                ? roleQueryPort.findById(command.roleId())
                : roleQueryPort.findByIdAndCompanyId(command.roleId(), companyId)).orElseThrow(
                        () -> new IllegalArgumentException("Role not found: " + command.roleId()));

        Optional<Long> disabledId = repository.findDisabledIdByEmployeeAndRole(command.employeeId(),
                command.roleId());
        if (disabledId.isPresent()) {
            Long id = disabledId.get();
            if (companyId == null) {
                repository.reactivate(id);
            } else {
                repository.reactivate(id, companyId);
            }
            EmployeeRole refreshed = (companyId == null
                    ? repository.findById(id)
                    : repository.findByIdAndCompanyId(id, companyId))
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
