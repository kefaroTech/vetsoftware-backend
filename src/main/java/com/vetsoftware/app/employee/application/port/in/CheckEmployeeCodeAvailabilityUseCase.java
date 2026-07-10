package com.vetsoftware.app.employee.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

/** Indica si un código de empleado está libre (chequeo en vivo del formulario). */
public interface CheckEmployeeCodeAvailabilityUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('employee.create')")
    boolean isAvailable(String employeeCode);
}
