package com.vetsoftware.app.employee.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Propone un código de empleado disponible a partir del nombre (prefijo = iniciales de la empresa).
 */
public interface SuggestEmployeeCodeUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('employee.create') and @authz.isMyCompany(#companyId))")
  String suggest(Long companyId, String name);
}
