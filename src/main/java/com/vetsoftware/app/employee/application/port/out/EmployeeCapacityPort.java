package com.vetsoftware.app.employee.application.port.out;

/** Reserva y libera puestos USER del contrato de la empresa. */
public interface EmployeeCapacityPort {
    void reserve(Long companyId);

    void release(Long companyId);
}
