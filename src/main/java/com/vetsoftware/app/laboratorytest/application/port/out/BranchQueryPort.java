package com.vetsoftware.app.laboratorytest.application.port.out;

import java.util.Optional;

public interface BranchQueryPort {

  /**
   * Id de la sede si pertenece a la empresa y está ACTIVA (para la sede solicitada al crear la
   * muestra).
   */
  Optional<Long> findActiveIdByIdAndCompanyId(Long branchId, Long companyId);

  /** Id de la sede activa por defecto de la empresa (Principal activa → si no, primera activa). */
  Optional<Long> findDefaultActiveIdByCompanyId(Long companyId);
}
