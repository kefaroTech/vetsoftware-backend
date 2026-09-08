package com.vetsoftware.app.subscriptionmodule.application.port.out;

/**
 * Si el empleado tiene, en esa empresa, el rol base {@code ADMIN} (D-9): el
 * único que puede comprar módulos. No es un permiso granular más —es el rol con
 * el que nace el dueño de la cuenta—, así que se resuelve contra
 * {@code roles.code}, no contra {@code EmployeeContext.permissions()}.
 */
public interface EmployeeAdminCheckPort {

    boolean isCompanyAdmin(Long employeeId, Long companyId);
}
