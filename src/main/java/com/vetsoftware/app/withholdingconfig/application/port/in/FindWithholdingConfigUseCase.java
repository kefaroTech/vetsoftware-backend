package com.vetsoftware.app.withholdingconfig.application.port.in;

import com.vetsoftware.app.withholdingconfig.application.dto.WithholdingConfigDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindWithholdingConfigUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('withholdingConfig.read') and @authz.isMyCompany(#companyId))")
    WithholdingConfigDto findByCompany(Long companyId);
}
