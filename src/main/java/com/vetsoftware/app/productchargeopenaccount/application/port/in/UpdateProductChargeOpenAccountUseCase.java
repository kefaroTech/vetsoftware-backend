package com.vetsoftware.app.productchargeopenaccount.application.port.in;

import com.vetsoftware.app.productchargeopenaccount.application.command.UpdateProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateProductChargeOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('chargeOpenAccount.update') and @authz.isMyCompany(#command.companyId)) or "
        + "hasRole('SYSTEM')")
    ProductChargeOpenAccountDto execute(UpdateProductChargeOpenAccountCommand command);
}
