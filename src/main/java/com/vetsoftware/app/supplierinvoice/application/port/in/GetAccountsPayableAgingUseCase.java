package com.vetsoftware.app.supplierinvoice.application.port.in;

import com.vetsoftware.app.supplierinvoice.application.dto.AccountsPayableAgingDto;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GetAccountsPayableAgingUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('supplierinvoice.read') and @authz.isMyCompany(#companyId))")
  AccountsPayableAgingDto get(Long companyId, Long branchId, LocalDate asOf);
}
