package com.vetsoftware.app.companysettings.application.port.in;

import com.vetsoftware.app.companysettings.application.dto.CompanySettingDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/** Lista los settings de la empresa. SOLO el admin (admin.all). */
public interface ListCompanySettingsUseCase {
    @PreAuthorize("hasAuthority('admin.all') and @authz.isMyCompany(#companyId)")
    List<CompanySettingDto> listByCompany(Long companyId);
}
