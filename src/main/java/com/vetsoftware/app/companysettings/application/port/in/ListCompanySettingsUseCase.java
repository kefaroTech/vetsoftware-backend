package com.vetsoftware.app.companysettings.application.port.in;

import com.vetsoftware.app.companysettings.application.dto.CompanySettingDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/** Lista los ajustes de una empresa con permiso granular y alcance tenant. */
public interface ListCompanySettingsUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('company.read') and @authz.isMyCompany(#companyId))")
  List<CompanySettingDto> listByCompany(Long companyId);
}
