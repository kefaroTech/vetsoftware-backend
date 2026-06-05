package com.vetsoftware.app.generalchargeopenaccount.application.port.in;

import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateGeneralChargeOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('chargeOpenAccount.delete') or "
        + "hasRole('SYSTEM')")
    GeneralChargeOpenAccountDto execute(Long id);
}
