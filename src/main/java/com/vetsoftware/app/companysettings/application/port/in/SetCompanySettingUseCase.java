package com.vetsoftware.app.companysettings.application.port.in;

import com.vetsoftware.app.companysettings.application.command.SetCompanySettingCommand;
import com.vetsoftware.app.companysettings.application.dto.CompanySettingDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** Togglear/ajustar un setting de empresa. SOLO el admin de la empresa (admin.all). */
public interface SetCompanySettingUseCase {
    @PreAuthorize("hasAuthority('admin.all') and @authz.isMyCompany(#command.companyId)")
    CompanySettingDto set(SetCompanySettingCommand command);
}
