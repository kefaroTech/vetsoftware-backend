package com.vetsoftware.app.productchargeopenaccount.application.port.in;

import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindProductChargeOpenAccountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('chargeOpenAccount.read')")
    ProductChargeOpenAccountDto findById(Long id, Long companyId);
}
