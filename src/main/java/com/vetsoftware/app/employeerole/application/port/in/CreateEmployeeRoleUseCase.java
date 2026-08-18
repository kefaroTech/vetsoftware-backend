package com.vetsoftware.app.employeerole.application.port.in;

import com.vetsoftware.app.employeerole.application.command.CreateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateEmployeeRoleUseCase {
    /**
     * El {@code isMyCompany} es defensa en profundidad y no la barrera: prueba que
     * el caller declara <em>su propia</em> empresa —el controller siempre la
     * inyecta desde el principal—, no de quien son el empleado y el rol que trae el
     * comando. Eso lo acota el servicio resolviendo las dos referencias con la
     * variante por empresa. Sin las dos cosas, {@code employee.create} bastaba para
     * asignarle un rol a un empleado de otra empresa adivinando su id.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('employee.create')"
            + " and @authz.isMyCompany(#command.companyId))")
    EmployeeRoleDto execute(CreateEmployeeRoleCommand command);
}
