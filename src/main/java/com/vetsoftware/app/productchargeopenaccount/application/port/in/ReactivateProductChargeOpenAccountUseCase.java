package com.vetsoftware.app.productchargeopenaccount.application.port.in;

import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateProductChargeOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('chargeOpenAccount.delete') or "
        + "hasRole('SYSTEM')")
    ProductChargeOpenAccountDto execute(Long id);
}
