package com.vetsoftware.app.dianprovider.application.port.in;

import com.vetsoftware.app.dianprovider.application.dto.DianProviderConfigDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindDianProviderConfigUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('electronicbilling.read') and @authz.isMyCompany(#companyId))")
    DianProviderConfigDto findByCompany(Long companyId);
}
