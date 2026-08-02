package com.vetsoftware.app.cashregister.application.port.in;

import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;

/** Lista las cajas OPEN de todas las sedes que el empleado autenticado puede consultar. */
public interface ListOpenCashSessionsUseCase {

  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('cashregister.read') and @authz.isMyCompany(#companyId))")
  List<CashSessionView> listOpen(Long companyId, Set<Long> accessibleBranchIds);
}
