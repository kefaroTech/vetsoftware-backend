package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.port.in.DeleteEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.application.port.out.EmployeeRolesQueryPort;
import com.vetsoftware.app.employee.domain.AdminEmployeeCannotBeDisabledException;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import com.vetsoftware.app.employee.domain.RoleSnapshot;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "employee.delete")
@Service
public class DeleteEmployeeService implements DeleteEmployeeUseCase {
    private static final String ADMIN_ROLE_CODE = "ADMIN";

    private final EmployeeRepository repository;
    private final EmployeeRolesQueryPort employeeRolesQueryPort;

    public DeleteEmployeeService(EmployeeRepository repository,
            EmployeeRolesQueryPort employeeRolesQueryPort) {
        this.repository = repository;
        this.employeeRolesQueryPort = employeeRolesQueryPort;
    }

    /**
     * {@code companyId} null = caller sin empresa (SYSTEM), cross-tenant por
     * diseño. Con empresa, la lectura previa va al finder acotado: el empleado de
     * otro tenant es un 404, no una baja. El {@code AND company_id} del UPDATE
     * cierra la misma puerta desde el SQL.
     */
    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        Employee employee = (companyId == null
                ? repository.findByIdIncludingDisabled(id)
                : repository.findByIdIncludingDisabledAndCompanyId(id, companyId))
                .orElseThrow(() -> new EmployeeNotFoundException(id));
        // Idempotente: si ya está desactivado, no hacemos nada (evita 404 en reintentos
        // / estado viejo
        // del front).
        if (!employee.isEnabled()) {
            return;
        }
        List<RoleSnapshot> roles = employeeRolesQueryPort.findRolesByEmployeeIds(List.of(id))
                .getOrDefault(id, List.of());
        if (roles.stream().anyMatch(r -> ADMIN_ROLE_CODE.equals(r.code()))) {
            throw new AdminEmployeeCannotBeDisabledException(id);
        }
        // Soft-delete del empleado. Conservamos sus asignaciones de rol: al reactivarlo
        // vuelven con él.
        // Un empleado desactivado queda inerte (no puede iniciar sesión), así que dejar
        // sus
        // employee_roles
        // activos no tiene efecto de seguridad.
        repository.delete(id, companyId);
    }
}
