package com.vetsoftware.app.generalchargeopenaccount.application.port.in;

import com.vetsoftware.app.generalchargeopenaccount.application.command.VoidGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface VoidGeneralChargeOpenAccountUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('chargeOpenAccount.delete') and"
          + " @authz.isMyCompany(#command.companyId)) or hasRole('SYSTEM')")
  GeneralChargeOpenAccountDto execute(VoidGeneralChargeOpenAccountCommand command);
}
