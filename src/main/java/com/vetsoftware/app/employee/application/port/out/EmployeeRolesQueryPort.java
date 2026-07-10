package com.vetsoftware.app.employee.application.port.out;

import com.vetsoftware.app.employee.domain.RoleSnapshot;
import java.util.List;
import java.util.Map;

public interface EmployeeRolesQueryPort {
    /** Roles vigentes (asignaciones activas) de cada empleado. */
    Map<Long, List<RoleSnapshot>> findRolesByEmployeeIds(List<Long> employeeIds);

    /** Roles para la pantalla de listado: vigentes para los activos y también las asignaciones (aunque
     *  deshabilitadas) de los empleados desactivados, para mostrar "el rol que tenían". */
    Map<Long, List<RoleSnapshot>> findRolesForListing(List<Long> employeeIds);
}
