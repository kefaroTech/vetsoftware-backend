package com.vetsoftware.app.branch.application.port.out;

import com.vetsoftware.app.branch.domain.Branch;
import java.util.List;
import java.util.Optional;

public interface BranchRepository {
  Branch save(Branch branch);

  Optional<Branch> findByIdAndCompanyId(Long id, Long companyId);

  List<Branch> findAllByCompanyId(Long companyId);

  /** ¿Ya existe ese código de sucursal en la empresa? (para validar unicidad al crear). */
  boolean codeExists(Long companyId, String code);

  /** Igual, excluyendo la propia sucursal (para validar unicidad al actualizar). */
  boolean codeExistsForOther(Long companyId, String code, Long id);

  /**
   * ¿Hay OTRA sucursal ACTIVA en la empresa distinta de {@code id}? Se usa para impedir desactivar
   * la última sede activa (dejaría a la empresa sin sede operativa y bloquearía citas/cuentas/POS).
   */
  boolean existsOtherActiveByCompanyId(Long companyId, Long id);
}
