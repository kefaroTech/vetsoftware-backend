package com.vetsoftware.app.servicechargeopenaccount.application.port.in;

import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListServiceChargeOpenAccountsByOpenAccountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('chargeOpenAccount.read') and @authz.isMyCompany(#companyId)) or "
            + "hasRole('SYSTEM')")
    List<ServiceChargeOpenAccountDto> listByOpenAccount(Long openAccountId, Long companyId);
}
