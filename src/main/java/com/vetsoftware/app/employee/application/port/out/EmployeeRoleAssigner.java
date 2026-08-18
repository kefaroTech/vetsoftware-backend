package com.vetsoftware.app.employee.application.port.out;

/**
 * Asigna un rol a un empleado y devuelve el nombre del rol (para el correo de
 * invitación).
 *
 * <p>
 * {@code companyId} no es decorativo: la feature {@code employeerole} necesita
 * la empresa para resolver acotadas las dos referencias (el empleado titular y
 * el rol). Sin ella, el alta de staff era la via por la que un administrador
 * podia asignar un rol de otro tenant. Mismo orden de parametros que
 * {@link EmployeeBranchAssigner#assign}.
 */
public interface EmployeeRoleAssigner {
    String assign(Long employeeId, Long companyId, Long roleId);
}
