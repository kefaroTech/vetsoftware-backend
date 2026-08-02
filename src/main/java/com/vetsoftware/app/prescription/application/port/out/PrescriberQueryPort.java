package com.vetsoftware.app.prescription.application.port.out;

import java.util.Optional;

/** Nombre del profesional que prescribe/imprime la fórmula (empleado autenticado). */
public interface PrescriberQueryPort {
  Optional<String> findName(Long employeeId);
}
