package com.vetsoftware.app.generalchargeopenaccount.application.port.in;

import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateGeneralChargeOpenAccountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('chargeOpenAccount.delete') and @authz.isMyCompany(#companyId)) or "
        + "hasRole('SYSTEM')")
    GeneralChargeOpenAccountDto execute(Long id, Long companyId);
}
