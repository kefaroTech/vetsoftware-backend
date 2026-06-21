package com.vetsoftware.app.withholdingconfig.application.port.in;

import com.vetsoftware.app.withholdingconfig.application.command.SetWithholdingConfigCommand;
import com.vetsoftware.app.withholdingconfig.application.dto.WithholdingConfigDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** Crea o actualiza (upsert) la configuración de tarifas de retención de la empresa. */
public interface SetWithholdingConfigUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('electronicbilling.create') and @authz.isMyCompany(#command.companyId))")
    WithholdingConfigDto execute(SetWithholdingConfigCommand command);
}
