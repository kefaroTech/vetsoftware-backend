package com.vetsoftware.app.debtopenaccount.application.port.in;

import com.vetsoftware.app.debtopenaccount.application.command.VoidDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface VoidDebtOpenAccountUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('debtOpenAccount.delete') and"
          + " @authz.isMyCompany(#command.companyId)) or hasRole('SYSTEM')")
  DebtOpenAccountDto execute(VoidDebtOpenAccountCommand command);
}
