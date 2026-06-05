package com.vetsoftware.app.generalchargeopenaccount.application.port.in;

import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListGeneralChargeOpenAccountsByOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('generalChargeOpenAccount.read') and @authz.isMyCompany(#companyId)) or "
        + "hasRole('SYSTEM')")
    List<GeneralChargeOpenAccountDto> listByOpenAccount(Long openAccountId, Long companyId);
}
