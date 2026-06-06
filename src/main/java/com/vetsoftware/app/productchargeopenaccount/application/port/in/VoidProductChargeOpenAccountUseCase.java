package com.vetsoftware.app.productchargeopenaccount.application.port.in;

import com.vetsoftware.app.productchargeopenaccount.application.command.VoidProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface VoidProductChargeOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('chargeOpenAccount.delete') and @authz.isMyCompany(#command.companyId)) or "
        + "hasRole('SYSTEM')")
    ProductChargeOpenAccountDto execute(VoidProductChargeOpenAccountCommand command);
}
