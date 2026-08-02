package com.vetsoftware.app.appointment.application.port.out;

import java.util.Optional;

/**
 * Nombre de la empresa (para el correo de confirmación de cita). La cita solo guarda el companyId.
 */
public interface CompanyQueryPort {
  Optional<String> findNameById(Long companyId);
}
