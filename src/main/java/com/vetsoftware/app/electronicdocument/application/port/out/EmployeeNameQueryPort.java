package com.vetsoftware.app.electronicdocument.application.port.out;

import java.util.Optional;

/**
 * B6 — Resuelve el nombre del empleado que emitió el documento (cajero del tiquete POS), para poblar los datos
 * reales del punto de venta en vez de un literal. Vacío si el id es null o el empleado no existe/está inactivo.
 */
public interface EmployeeNameQueryPort {
    Optional<String> findName(Long employeeId);
}
