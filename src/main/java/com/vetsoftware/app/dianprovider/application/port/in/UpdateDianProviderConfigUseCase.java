package com.vetsoftware.app.dianprovider.application.port.in;

import com.vetsoftware.app.dianprovider.application.command.UpdateDianProviderConfigCommand;
import com.vetsoftware.app.dianprovider.application.dto.DianProviderConfigDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateDianProviderConfigUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('electronicbilling.update') and"
            + " @authz.isMyCompany(#command.companyId))")
    DianProviderConfigDto execute(UpdateDianProviderConfigCommand command);
}
