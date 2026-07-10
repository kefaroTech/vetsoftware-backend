package com.vetsoftware.app.employee.application.port.out;

/** Asigna un rol a un empleado y devuelve el nombre del rol (para el correo de invitación). */
public interface EmployeeRoleAssigner {
    String assign(Long employeeId, Long roleId);
}
