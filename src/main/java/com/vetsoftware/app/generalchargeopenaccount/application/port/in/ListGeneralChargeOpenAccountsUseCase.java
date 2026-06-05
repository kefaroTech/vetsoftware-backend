package com.vetsoftware.app.generalchargeopenaccount.application.port.in;

import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListGeneralChargeOpenAccountsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<GeneralChargeOpenAccountDto> listAll();
}
