package com.vetsoftware.app.dianprovider.application.port.in;

import com.vetsoftware.app.dianprovider.application.command.CreateDianProviderConfigCommand;
import com.vetsoftware.app.dianprovider.application.dto.DianProviderConfigDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateDianProviderConfigUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('electronicbilling.create') and"
          + " @authz.isMyCompany(#command.companyId))")
  DianProviderConfigDto execute(CreateDianProviderConfigCommand command);
}
