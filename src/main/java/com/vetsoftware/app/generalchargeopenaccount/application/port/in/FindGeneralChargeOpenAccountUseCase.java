package com.vetsoftware.app.generalchargeopenaccount.application.port.in;

import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindGeneralChargeOpenAccountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('chargeOpenAccount.read')")
    GeneralChargeOpenAccountDto findById(Long id, Long companyId);
}
