package com.vetsoftware.app.servicechargeopenaccount.application.port.in;

import com.vetsoftware.app.servicechargeopenaccount.application.command.CreateServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateServiceChargeOpenAccountUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('chargeOpenAccount.create') and"
          + " @authz.isMyCompany(#command.companyId)) or hasRole('SYSTEM')")
  ServiceChargeOpenAccountDto execute(CreateServiceChargeOpenAccountCommand command);
}
