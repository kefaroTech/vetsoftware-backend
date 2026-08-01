package com.vetsoftware.app.productchargeopenaccount.application.port.in;

import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListProductChargeOpenAccountsByOpenAccountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('chargeOpenAccount.read') and @authz.isMyCompany(#companyId)) or "
        + "hasRole('SYSTEM')")
    List<ProductChargeOpenAccountDto> listByOpenAccount(Long openAccountId, Long companyId);
}
