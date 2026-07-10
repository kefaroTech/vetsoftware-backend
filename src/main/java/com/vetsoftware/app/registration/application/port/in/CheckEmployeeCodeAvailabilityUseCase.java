package com.vetsoftware.app.registration.application.port.in;

/** Indica si un código de acceso está libre para usarse en el registro (Opción A). */
public interface CheckEmployeeCodeAvailabilityUseCase {
    boolean isAvailable(String employeeCode);
}
