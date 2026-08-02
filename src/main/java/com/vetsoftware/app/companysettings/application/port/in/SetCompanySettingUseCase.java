package com.vetsoftware.app.companysettings.application.port.in;

import com.vetsoftware.app.companysettings.application.command.SetCompanySettingCommand;
import com.vetsoftware.app.companysettings.application.dto.CompanySettingDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** Modifica un ajuste empresarial con permiso granular y alcance tenant. */
public interface SetCompanySettingUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('company.update') and @authz.isMyCompany(#command.companyId))")
    CompanySettingDto set(SetCompanySettingCommand command);
}
