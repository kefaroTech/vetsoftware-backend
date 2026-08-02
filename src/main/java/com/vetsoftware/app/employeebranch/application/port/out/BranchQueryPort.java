package com.vetsoftware.app.employeebranch.application.port.out;

import java.util.List;

public interface BranchQueryPort {

  /**
   * Ids de todas las sedes de la empresa (activas e inactivas) — para expandir "todas" y validar el
   * set pedido.
   */
  List<Long> findBranchIdsByCompanyId(Long companyId);
}
