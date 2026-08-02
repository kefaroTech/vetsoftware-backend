package com.vetsoftware.app.passwordreset.application.port.out;

import java.util.Optional;

/**
 * Busca la cuenta de empleado por su código de acceso, para armar el correo de restablecimiento.
 * Solo devuelve empleados activos (habilitados); el service filtra además por correo verificado.
 */
public interface EmployeeAccountLookupPort {
  Optional<EmployeeAccount> findByCode(String employeeCode);

  record EmployeeAccount(
      Long id,
      Long companyId,
      String name,
      String email,
      String companyName,
      boolean emailVerified) {}
}
