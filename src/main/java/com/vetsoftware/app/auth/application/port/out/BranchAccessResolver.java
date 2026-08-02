package com.vetsoftware.app.auth.application.port.out;

import java.util.Set;

public interface BranchAccessResolver {

  /**
   * Ids de las sedes a las que el empleado tiene acceso. {@code employee_branches} es la única
   * fuente: "todas las sedes" se materializa como una fila por sede (no hay flag). Un set vacío =
   * sin acceso a recursos scopeados a sede. El acceso total se representa materializando todas las
   * sedes, no mediante un permiso comodín.
   */
  Set<Long> resolveFor(Long employeeId);
}
